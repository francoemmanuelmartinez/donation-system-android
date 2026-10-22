# Plan — Recordatorios interactivos por WhatsApp (OpenWA)

**Fecha:** 2026-10-22
**Estado:** propuesta, pendiente de aprobación (no implementado).
**Alcance:** llevar los recordatorios de WhatsApp de "solo texto saliente" a un canal **interactivo**: mensajes con **poll/botones** para que el donante confirme, cancele o pida reagendar, y **webhook entrante** que refleje esas respuestas en la app. Complementa a `plan-agenda-robusta.md` (no lo duplica: allí el reagendado es manual; acá el donante lo **solicita**).
**Depende de:** `plan-agenda-robusta.md` (estados, `cancel_appointment`, `reschedule_requested_at`, filtro de Agenda).

## Objetivo
Que el donante pueda responder por WhatsApp y que esa respuesta tenga efecto (o quede para revisión) en la app, sin que nadie copie y pegue nada.

## Contexto técnico verificado
- Transporte actual: `backend/whatsapp_sender.php:53` `sendViaOpenWa($number, $text)` → `POST /api/sessions/{SESSION_ID}/messages/send-text` (texto libre). Config en `config_reminders.php:36` / `config_reminders_override.php:7` (`OPENWA_BASE_URL`, `OPENWA_SESSION_ID`).
- Gateway OpenWA (Node + Baileys) expone (verificado en `C:\Users\games\openwa\OpenWA\openapi.json`):
  - `POST /api/sessions/{sessionId}/messages/send-poll`
  - `POST /api/sessions/{sessionId}/messages/click-button`
  - `GET|POST /api/sessions/{sessionId}/webhooks`
- **Limitación real:** los "botones nativos" de WhatsApp Business no son soportados de forma confiable por motores tipo Baileys. Lo viable es **poll** (el donante "vota" una opción) o texto libre interpretado. Además, el **texto libre saliente solo se entrega dentro de la ventana de 24 h**; fuera de eso hace falta **plantilla UTILITY** (`send-template` ya existe en el gateway).
- La app Android **no** consume OpenWA; todo pasa por el backend.

## Concepto de flujo
```
Backend (recordatorio) ──send-poll──▶ Donante
                                        │ elige opción
                                        ▼
OpenWA ──webhook POST──▶ Endpoint receptor (nuevo) ──▶ DB (turno)
                                        │
                                        ▼
                              App: Agenda refleja el cambio / solicitud
```

## Decisiones resueltas
- **R1 — Canal de respuesta:** **poll** como mecanismo principal (más confiable que botones en Baileys). Texto libre interpretado como fallback opcional.
- **R2 — Entrega garantizada:** introducir **plantilla UTILITY** para el recordatorio base; el poll se envía como mensaje interactivo cuando hay ventana, o se referencia desde la plantilla. Priorizar plantilla por confiabilidad.
- **R3 — Confirmar:** si el donante confirma, no cambia nada en la Agenda (turno sigue `SCHEDULED`), pero se marca `reminder_confirmed_at` para seguimiento.
- **R4 — Cancelar:** la respuesta de cancelación **refleja directo** en el turno (`status='CANCELLED'`, `cancel_reason='DONOR_CANCELLED'`), reutilizando la lógica de T2/T4 de la agenda.
- **R5 — Reagendar:** la respuesta **no** auto-reagenda; setea `reschedule_requested_at` y el turno aparece en el modo "Solicitudes" de la Agenda (T10). El operador reagenda manualmente.
- **R6 — Seguridad del webhook:** endpoint con **secreto compartido** (header/HMAC), sin exponer datos sensibles en la URL; idempotencia por `messageId`/`pollId` (WhatsApp reintenta).
- **R7 — Reversibilidad:** toda acción automática deja auditoría (`cancelled_by_user_id = NULL`, `cancel_reason='DONOR_CANCELLED'`, timestamp de la respuesta).
- **R8 — Opt-in/errores:** si el donante no tiene teléfono primario o el envío falla, se registra y no se bloquea el resto del dispatch.

## Fases

### Fase 1 — Poll saliente + recepción de webhook (núcleo)
1. **Emisión:** al enviar el recordatorio, usar `send-poll` con la pregunta:
   ```
   ¿Podés asistir a tu turno del 28/10 10:30?
     ○ Confirmo asistencia
     ○ Necesito cancelar
     ○ Necesito reagendar
   ```
   Guardar `pollId`/`messageId` y el `appointmentId` asociado para poder mapear la respuesta.
2. **Recepción:** endpoint nuevo `backend/whatsapp_webhook.php` (POST) que:
   - valida el secreto compartido;
   - ignora reintentos ya procesados (idempotencia por `messageId`);
   - mapea la votación al `appointmentId` (por `pollId` guardado);
   - aplica R3/R4/R5 sobre el turno.
3. **Config:** alta del webhook en OpenWA (`POST /api/sessions/{SESSION_ID}/webhooks`) apuntando al endpoint.

### Fase 2 — Entrega garantizada (plantilla)
1. Definir plantilla UTILITY (recordatorio de turno).
2. `sendViaMeta`/`send-template` según configuración; mantener poll cuando haya ventana de 24 h.

### Fase 3 — Robustez
1. Panel/estado de respuestas (confirmó/canceló/pidió reagendar) y reintentos de envío.
2. Métricas (tasa de respuesta, entregas fallidas) reutilizando `stats` del gateway.

## Tickets

### R-1 — Esquema de seguimiento de recordatorio
**Bloqueado por:** `plan-agenda-robusta.md` T1 (misma migración o una nueva).
**Tareas:** agregar `reminder_message_id`, `reminder_poll_id`, `reminder_response`, `reminder_responded_at`, `reminder_confirmed_at` a `appointments` (o tabla `reminder_responses`).
**Aceptación:** se puede mapear una respuesta del donante a un turno.

### R-2 — Emisión de poll
**Bloqueado por:** R-1
**Archivos:** `backend/whatsapp_sender.php`, `backend/send_reminders.php`.
**Tareas:** función `sendPollViaOpenWa($number, $question, $options)`; usarla en el recordatorio; persistir ids (R-1).
**Aceptación:** el donante recibe el poll; se guardan `pollId`/`messageId`.

### R-3 — Endpoint receptor (webhook)
**Bloqueado por:** R-1
**Archivos:** `backend/whatsapp_webhook.php` (nuevo), `backend/appointments_lib.php`.
**Tareas:** validar secreto; idempotencia; mapear respuesta→turno; aplicar R3/R4/R5 reutilizando `cancelAppointment()`/marca de solicitud.
**Aceptación:** respuesta "cancelar" cancela el turno; "reagendar" setea `reschedule_requested_at`; reintentos no duplican efectos; request sin secreto → 401.

### R-4 — Registro del webhook en OpenWA
**Bloqueado por:** R-3
**Tareas:** script/helpers para `POST /api/sessions/{SESSION_ID}/webhooks` con la URL pública del backend y el secreto; documentar en `whatsapp-reminders-guide.md`.
**Aceptación:** el gateway entrega eventos al endpoint; se ve en los logs de entrega.

### R-5 — Plantilla UTILITY (fase 2)
**Bloqueado por:** — 
**Tareas:** definir plantilla; ruta de envío por plantilla; documentar credenciales.
**Aceptación:** el recordatorio se entrega fuera de la ventana de 24 h.

### R-6 — App: reflejo de respuestas
**Bloqueado por:** R-3, `plan-agenda-robusta.md` T10
**Tareas:** la Agenda ya refleja cancelaciones (filtro activo/histórico) y solicitudes ("Solicitudes"); opcional: chip "Confirmó asistencia".
**Aceptación:** una cancelación por WhatsApp desaparece del activo; una solicitud aparece en "Solicitudes".

### R-7 — Documentación y pruebas
**Bloqueado por:** R-3
**Tareas:** actualizar `whatsapp-reminders-guide.md`; tests de idempotencia y mapeo; simular webhook con `POST /webhooks/{id}/test`.
**Aceptación:** guía con el flujo completo y pruebas de webhook en verde.

## Verificación
1. `php -l` de los PHP tocados.
2. Simulación de webhook (`/webhooks/{id}/test` y payload manual) → efectos esperados.
3. Reintento duplicado → sin efecto doble (idempotencia).
4. QA manual: recordatorio → respuesta confirmar/cancelar/reagendar → Agenda consistente.

## Fuera de alcance
- Botones nativos de WhatsApp (no confiables en Baileys).
- Auto-reagendado por el donante (siempre lo revisa un operador, R5).
- Multi-idioma de plantillas.

## Riesgos
- **Seguridad del webhook:** endpoint público que toca turnos. Mitigación: secreto compartido + idempotencia + whitelist de IP si es posible.
- **Reintentos de WhatsApp:** sin idempotencia se duplican efectos. Mitigación: dedupe por `messageId`.
- **Ventana de 24 h:** poll puede no entregarse; mitigación: plantilla UTILITY (fase 2).
- **Acoplamiento:** no mezclar esta integración con el lifecycle de la Agenda; consumir solo los endpoints de `appointments_lib.php`.
