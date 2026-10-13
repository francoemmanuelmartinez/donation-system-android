# Plan de migración: WhatsApp → Cloud API oficial de Meta

> Proyecto: DonationSystem (banco de sangre) — recordatorios automáticos 2 días antes del turno.

> ## 🛑 EN PAUSA (12-oct-2026)
> **Decisión del usuario: frenar la migración a Meta y seguir con OpenWA como único proveedor operativo.**
> Motivo: el Business Portfolio creado quedó **restringido por hold de cuenta de Facebook nueva** (no figura en "Calidad de la cuenta"; es hold de confianza por antigüedad, sin botón de revisión). Meta no deja generar la WABA ni el número de prueba.
> Se retomará Fase 0–3 solo si el hold se libera. El código de Fase 2 queda listo e **inerte** (`META_TOKEN` vacío → `sendViaMeta()` nunca se invoca con el proveedor `openwa`).

> Estado operativo: proveedor **OpenWA** (Baileys, no oficial) funcionando y enviando normal.

## Resumen de impacto

| Aspecto | Hoy (OpenWA) | Con Cloud API de Meta |
|---|---|---|
| Servicios locales | OpenWA (node, puerto 2785) + dashboard (2886) | Ninguno (se apagan; XAMPP/PHP sigue igual) |
| Envío | Texto libre + saludos random | Plantilla UTILITY aprobada con variables {{1}}..{{4}} |
| Riesgo de baneo | Alto (WhatsApp Web no oficial) | Cero (oficial) |
| Costo | $0 | ~US$1,37/mes (50 msgs utility AR ≈ $2.700 ARS a dólar tarjeta) |
| QR / sesión / warm-up | Sí | Innecesarios |

## Lo que NO cambia
- Scheduler / lazy sweep, selección de candidatos, anti-ráfaga, retry, logs, lock.
- La app Android. XAMPP/Apache/PHP (solo un `curl` HTTPS a `graph.facebook.com`).
- Formato de número (E.164, ya lo resuelve `normalizeWaNumber()`).

## Fase 0 — Trámites en Meta (de cero, sin instalar nada)
1. **Meta Business Portfolio** (business.facebook.com), alta a título de prueba con el nombre **`App Donaciones Demo`** (decidido 12-oct-2026: nombre que no sugiere afiliación con un banco real, evita que Meta pida documentación de institución). **No hace falta ser empresa:** un individuo puede crearlo con su cuenta de Facebook; la verificación de negocio no es obligatoria para volumen bajo (límite inicial ≈250 conversaciones/día, muy por encima de nuestras 2/día).
2. **WABA** (WhatsApp Business Account) dentro del Portfolio. Para volumen bajo no exige verificación completa; con display name aprobado se arranca en 2.000 conversaciones/día (regla desde oct-2025).
3. **App de desarrollo** (developers.facebook.com) tipo Business + producto WhatsApp → **test number (sandbox): 100 % GRATIS, sin cargar tarjeta ni método de pago**. Permite enviar a hasta **5 destinatarios verificados** (sirve para probar sin comprometer el número real). El sandbox **no cobra por mensaje**: la facturación solo aplica a números reales registrados en producción.
4. **Número de producción**: registrar el número **dedicado** → verificación por SMS/llamada + **PIN de 2 pasos**.
5. **Token permanente de System User** (Business Settings → System Users) con permisos `whatsapp_business_messaging` + `whatsapp_business_management`, y **display name** aprobado (el nombre del banco).
6. **Facturación** (Meta Billing → Messaging accounts): método de pago. **Solo tarjetas Visa o Mastercard físicas emitidas por un banco** (crédito o débito). ❌ No acepta Amex, PayPal, transferencia bancaria, ni tarjetas prepago/virtuales. Requiere **CUIT/CUIL** como dato fiscal. Se cobra solo por plantilla entregada en producción; entrantes gratis. Tarifa UTILITY/AR vigente oct-2026: **US$0,0273 por mensaje entregado** (≈ US$1,37/mes por 50 mensajes ≈ $2.100–2.750 ARS según cotización; ver "Costos").
7. **Webhooks: NO se implementan** (solo envío saliente; localhost no expone HTTPS público).

## Costos (estimados 12-oct-2026)
- **Sandbox/test number: $0.** Sin tarjeta, sin cargo por mensaje.
- **Producción:** solo por plantilla UTILITY entregada en AR → **US$0,0273/mensaje**. 50/mes ≈ **US$1,37/mes**.
  - En ARS según cotización: dólar tarjeta (~$2.008) → **≈ $2.700/mes**; dólar blue ($1.540) → ≈ $2.110/mes; oficial/MEP (~$1.545-1.552) → ≈ $2.110-2.120/mes.
  - No se cobran mensajes no entregados. No hay mensualidad ni costo de plataforma (Cloud API directa, sin BSP).

## Fase 1 — Plantilla UTILITY (redactada y decidida)
- **Nombre:** `recordatorio_turno` (minúsculas + guiones bajos).
- **Idioma:** `es_AR`. **Categoría:** UTILITY (recordatorio de cita → aprobación rápida).
- **Cuerpo definitivo (neutro, sin nombrar banco — decidido 12-oct-2026):**
  > Hola {{1}}, le recordamos su turno de {{2}} el {{3}} a las {{4}}. Si no puede asistir, por favor avísenos. Responda "BAJA" para dejar de recibir avisos.

- Variables: {{1}} nombre completo, {{2}} procedimiento (SANGRE/PLASMA/PLAQUETAS), {{3}} fecha ("lunes 13/10/2026"), {{4}} hora ("10:30"). Se completan con los datos que ya genera `buildReminderMessage()` / `buildMetaParams()`.
- El texto libre (`REMINDER_MESSAGE_TEMPLATE` + `$REMINDER_GREETINGS`) **deja de usarse** para envíos proactivos (regla de Meta).

### Creación y aprobación de plantillas
- **100 % GRATIS**: crear, editar y aprobar plantillas no tiene costo ni límite de intentos. No se cobra por aprobación.
- **Requisito de pre-aprobación**: una plantilla solo se puede enviar con estado **APPROVED** (nada de envíos pendientes, ni en sandbox ni en producción). Estados: PENDING → APPROVED / REJECTED.
- **UTILITY se aprueba rápido** (minutos-horas típicos). Si la rechazan: corregir el copy y reenviar.
- **En el sandbox también se usan plantillas**: el test number trae la de ejemplo pre-aprobada (`hello_world`) para validar el circuito al instante, y las custom (como `recordatorio_turno`) se pueden crear en la WABA en modo dev y usarlas desde el test number una vez APPROVED.

## Fase 2 — Código (ESTADO: implementado el 12-oct-2026, pendiente solo credenciales)
- `config_reminders.php`: **hecho** — bloque `META_API_VERSION='v21.0'`, `META_TOKEN=''`, `META_PHONE_NUMBER_ID=''`, `META_TEMPLATE_NAME='recordatorio_turno'`, `META_TEMPLATE_LANGUAGE='es_AR'`. `REMINDER_PROVIDER` sigue en `'openwa'`.
- `whatsapp_sender.php`: **hecho** — firma `sendWhatsappText($number, $text, $params = null)`; `sendViaMeta()` completo (body template con `components[body].parameters` posicionales `[[{{1}}],[{{2}}],[{{3}}],[{{4}}]]`, parseo de `error.message`, mensaje claro si faltan credenciales o hay variables vacías). `sendViaOpenWa()` intacto, ignora `$params`.
- `send_reminders.php`: **hecho** — nuevo `buildMetaParams()` y el dispatcher pasa `$metaParams` (nombre/procedimiento/fecha/hora) a `sendWhatsappText()` en el envío y el reintento.
- Retry, logs, lock, ventana, warm-up, pacing: **sin cambios**.
- **Pendiente (no es código):** completar `META_TOKEN` / `META_PHONE_NUMBER_ID` y cambiar `REMINDER_PROVIDER='meta'` cuando haya WABA + plantilla aprobados.

## Fase 3 — Pruebas y corte
1. Probar con **test number sandbox + 5 destinatarios verificados**: el circuito completo se valida **sin costo ni tarjeta**, usando primero `hello_world` (al instante) y luego `recordatorio_turno` (una vez APPROVED).
2. Alta y aprobación de plantilla en la WABA de producción (gratis).
3. Envío de prueba real con el número dedicado (a tu número).
4. Cambiar `REMINDER_PROVIDER='meta'` y apagar OpenWA (node/dashboard), que queda como fallback.

## Modo dual / Fallback rápido (OpenWA ↔ Meta)
El proyecto mantiene **ambos proveedores implementados en paralelo**. El cambio es de config (una línea), sin tocar app Android, BD ni scheduling. Decisión del banco: operar gratis (OpenWA) o de pago-oficial (Meta).

### Reglas de convivencia de números
- **Meta** → número dedicado del banco (producción).
- **OpenWA** → chip dedicado 5492233556122 (sesión `test-recordatorios`, id `52906cda-d122-425f-abf0-58530793e3d7`; cambio 12-oct-2026).
- No hay conflicto: son números distintos. El fallback de OpenWA queda operativo aunque Meta sea el primario.
- No registrar el mismo número en ambos (números iguales = conflicto de cuenta).

### Matriz de conmutación
| Cambio | Pasos |
|---|---|
| OpenWA → Meta | 1) `REMINDER_PROVIDER='meta'` en `config_reminders.php` · 2) token + plantilla aprobados |
| Meta → OpenWA | 1) `REMINDER_PROVIDER='openwa'` · 2) verificar servicio `OpenWA-Gateway` Running + `curl http://localhost:2785/api/health` |

### Qué NO se toca en la Fase 3 (para no perder el fallback)
1. **No desinstalar OpenWA** → el gateway corre como servicio Windows `OpenWA-Gateway` (NSSM): `Stop-Service OpenWA-Gateway` para pausar, `Start-Service` para volver; dashboard manual con `openwa-dashboard.bat`. Para el conmutador basta con `REMINDER_PROVIDER`.
2. **No borrar** el bloque `OPENWA_*` de `config_reminders.php` ni `sendViaOpenWa()` en `whatsapp_sender.php`.
3. `send_reminders.php` seguirá generando **texto + variables**: OpenWA usa el texto, Meta las variables. Ambos conviven en `sendWhatsappText($number, $text, $params)`.
4. `is_reminder_sent` es compartido → conmutar no duplica envíos.
5. Cheat-sheet de conmutación queda documentado en `whatsapp-reminders-guide.md` §7.12.

### Estado esperado al terminar
`config_reminders.php` contiene **ambos bloques** (`OPENWA_*` y `META_*`) y el proveedor activo se elige por `REMINDER_PROVIDER`. La operación puede alternar en <1 min.

## Respuestas confirmadas (12-oct-2026)
1. **Sandbox gratis:** el test number no cobra ni exige tarjeta; sirve solo para pruebas (máx. 5 destinatarios verificados), no para producción.
2. **Plantillas contra el sandbox:** sí se usan (muestra `hello_world` al instante + custom `recordatorio_turno` una vez aprobada).
3. **Pre-aprobación obligatoria:** solo se envía con estado APPROVED (también en sandbox).
4. **Crear/aprobar plantillas no cuesta nada:** 100 % gratis.
5. **Uso a título personal (sin empresa):** alcanza crear el Portfolio con tu Facebook y un nombre de negocio; facturación con CUIT/CUIL y una Visa/Mastercard física.
6. **Costo estimado producción:** US$0,0273/mensaje utility AR → 50/mes ≈ US$1,37 ≈ $2.100-$2.750 ARS.

## Decisiones tomadas (12-oct-2026)
1. **Número Meta:** dedicado del banco (punto de retomar si se desbloquea el hold). OpenWA (el operativo) ya usa el chip dedicado 5492233556122; el personal 5492235024735 vuelve al uso personal y queda fuera del sistema.
2. **Display name / nombre de negocio:** `App Donaciones Demo` (evita parecer afiliado a un banco real y que Meta pida documentación).
3. **Plantilla:** neutra, sin "Banco de Sangre" (aprobación igual, copy listo en Fase 1).
4. **Fase 2:** implementada completa hoy; queda pendiente solo cargar credenciales (token + phone_number_id) cuando exista la WABA.

## Pendientes (acciones de Meta / credenciales) — EN PAUSA
1. **Desbloquear el hold del Business Portfolio**: cuenta FB de este mes → completar email/teléfono/foto en el perfil FB, confirmar identidad en Security Center y esperar 5–7 días sin reintentos. Luego crear WABA + test number (display name `App Donaciones Demo`).
2. Registrar el **número dedicado** (verificación SMS/llamada + PIN 2 pasos) + token System User.
3. Crear y aprobar la plantilla `recordatorio_turno` (es_AR, UTILITY, cuerpo neutro de la Fase 1).
4. Cargar Visa/Master física + CUIT/CUIL en Meta Billing.
5. Fase 3: probar con sandbox (`hello_world` → `recordatorio_turno`), luego producción, luego `REMINDER_PROVIDER='meta'`.