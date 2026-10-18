package com.tuapp;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente HTTP sin librerías: usa HttpURLConnection + org.json.
 * Los tres pedidos que hablan con reminders_api.php:
 *   obtenerPendientes() -> GET  ?action=pending
 *   enviar(id)          -> POST ?action=send
 *   dispatch()          -> POST ?action=dispatch
 *   crear()             -> POST ?action=crear
 *   resetGate()         -> POST ?action=reset
 */
public class RecordatoriosApi {

    // TODO: cambiá esto por la dirección real de tu backend (¡con la barra al final!)
    // Ejemplo con el emulador de Android Studio: http://10.0.2.2/tu-backend/ (ver guía, D.0)
    private static final String BASE = "http://TU-SERVIDOR/tu-backend/";

    // "Callback" = una forma de avisar "ya terminé, acá está el resultado".
    public interface CallbackPendientes { void onListo(List<Recordatorio> lista, String error); }
    public interface CallbackAccion     { void onListo(boolean ok, String mensaje); }

    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());

    // 1) Traer los pendientes
    public void obtenerPendientes(final CallbackPendientes callback) {
        new Thread(() -> {
            try {
                String respuesta = get(BASE + "reminders_api.php?action=pending");
                JSONObject json = new JSONObject(respuesta);
                if (!json.optBoolean("ok")) {
                    final String err = json.optString("error", "Error del servidor");
                    hiloPrincipal.post(() -> callback.onListo(null, err));
                    return;
                }
                JSONArray arr = json.getJSONArray("data");
                final List<Recordatorio> lista = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    lista.add(new Recordatorio(
                            o.getInt("id"),
                            o.optString("destinatario_nombre"),
                            o.optString("telefono"),
                            o.optString("mensaje"),
                            o.optString("programado_para")));
                }
                hiloPrincipal.post(() -> callback.onListo(lista, null));
            } catch (Exception e) {
                hiloPrincipal.post(() -> callback.onListo(null, e.getMessage()));
            }
        }).start();
    }

    // 2) Enviar UN recordatorio por su id
    public void enviar(final int id, final CallbackAccion callback) {
        new Thread(() -> {
            try {
                JSONObject cuerpo = new JSONObject();
                cuerpo.put("id", id);
                String respuesta = post(BASE + "reminders_api.php?action=send", cuerpo.toString());
                JSONObject json = new JSONObject(respuesta);
                final boolean ok = json.optBoolean("ok");
                final String msg = ok ? "Enviado" : json.optString("error", "Error");
                hiloPrincipal.post(() -> callback.onListo(ok, msg));
            } catch (Exception e) {
                hiloPrincipal.post(() -> callback.onListo(false, e.getMessage()));
            }
        }).start();
    }

    // 3) Enviar TODOS los pendientes en segundo plano
    public void dispatch(final CallbackAccion callback) {
        new Thread(() -> {
            try {
                String respuesta = post(BASE + "reminders_api.php?action=dispatch", "{}");
                JSONObject json = new JSONObject(respuesta);
                final boolean ok = json.optBoolean("ok");
                final String msg = ok ? "Envío lanzado" : json.optString("error", "Error");
                hiloPrincipal.post(() -> callback.onListo(ok, msg));
            } catch (Exception e) {
                hiloPrincipal.post(() -> callback.onListo(false, e.getMessage()));
            }
        }).start();
    }

    // 4) Crear recordatorio en BD (con delay en minutos)
    public void crear(final String nombre, final String telefono, final String mensaje,
                      final int minutos, final CallbackAccion callback) {
        new Thread(() -> {
            try {
                JSONObject cuerpo = new JSONObject();
                cuerpo.put("nombre", nombre);
                cuerpo.put("telefono", telefono);
                cuerpo.put("mensaje", mensaje);
                cuerpo.put("minutos", minutos);
                String respuesta = post(BASE + "reminders_api.php?action=crear", cuerpo.toString());
                JSONObject json = new JSONObject(respuesta);
                final boolean ok = json.optBoolean("ok");
                final String msgResp = ok ? "Recordatorio creado" : json.optString("error", "Error");
                hiloPrincipal.post(() -> callback.onListo(ok, msgResp));
            } catch (Exception e) {
                hiloPrincipal.post(() -> callback.onListo(false, e.getMessage()));
            }
        }).start();
    }

    // 5) Reiniciar gate del lazy sweep (solo demo)
    public void resetGate(final CallbackAccion callback) {
        new Thread(() -> {
            try {
                String respuesta = post(BASE + "reminders_api.php?action=reset", "{}");
                JSONObject json = new JSONObject(respuesta);
                final boolean ok = json.optBoolean("ok");
                final String msgResp = ok ? "Gate reiniciado" : json.optString("error", "Error");
                hiloPrincipal.post(() -> callback.onListo(ok, msgResp));
            } catch (Exception e) {
                hiloPrincipal.post(() -> callback.onListo(false, e.getMessage()));
            }
        }).start();
    }

    private String get(String direccion) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(direccion).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);
        return leer(c);
    }

    private String post(String direccion, String cuerpo) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(direccion).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        c.setConnectTimeout(5000);
        c.setReadTimeout(5000);
        OutputStream os = c.getOutputStream();
        os.write(cuerpo.getBytes("UTF-8"));
        os.close();
        return leer(c);
    }

    private String leer(HttpURLConnection c) throws Exception {
        int codigo = c.getResponseCode();
        InputStream is = (codigo >= 200 && codigo < 300) ? c.getInputStream() : c.getErrorStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String linea;
        while ((linea = br.readLine()) != null) {
            sb.append(linea);
        }
        br.close();
        return sb.toString();
    }
}
