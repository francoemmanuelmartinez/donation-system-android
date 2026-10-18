package com.tuapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla mínima que demuestra las 3 funcionalidades:
 *
 *   1. Botón "Enviar pendientes"  -> OpenWA (servidor). Lee y envía TODOS los
 *                                      recordatorios pendientes guardados en la BD.
 *   2. Botón "Abrir WhatsApp"     -> wa.me (celular). Usa el PRIMER pendiente de la BD.
 *   3. Automático al abrir         -> pide 'pending' (lazy sweep). Se ejecuta 1 vez/día.
 *
 * Para usarla en tu proyecto:
 *   - Cambiá el "package" en los 3 archivos .java
 *   - Copiá activity_recordatorios.xml a res/layout/
 *   - Agregá los permisos del README a AndroidManifest.xml
 *   - Registrate la Activity en el manifest (ver README)
 */
public class RecordatoriosActivity extends AppCompatActivity {

    private final RecordatoriosApi api = new RecordatoriosApi();
    private TextView textoPendientes;
    private EditText editNombre;
    private EditText editTelefono;
    private EditText editMensaje;
    private Spinner spinnerMinutos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordatorios);

        textoPendientes = findViewById(R.id.textoPendientes);
        editNombre = findViewById(R.id.editNombre);
        editTelefono = findViewById(R.id.editTelefono);
        editMensaje = findViewById(R.id.editMensaje);
        spinnerMinutos = findViewById(R.id.spinnerMinutos);

        Button botonCrear = findViewById(R.id.botonCrear);
        Button botonWaMeForm = findViewById(R.id.botonWaMeForm);
        Button botonPendientes = findViewById(R.id.botonPendientes);
        Button botonWaMe = findViewById(R.id.botonWaMe);
        Button botonResetGate = findViewById(R.id.botonResetGate);

        // Spinner minutos
        List<String> items = new ArrayList<>();
        items.add("0 min (ahora)");
        items.add("1 min");
        items.add("5 min");
        items.add("10 min");
        items.add("15 min");
        items.add("30 min");
        items.add("60 min");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, items);
        spinnerMinutos.setAdapter(adapter);

        // ② Crear recordatorio en BD
        botonCrear.setOnClickListener(v -> {
            String nombre = editNombre.getText().toString().trim();
            String telefono = editTelefono.getText().toString().trim();
            String mensaje = editMensaje.getText().toString().trim();
            if (nombre.isEmpty() || telefono.isEmpty() || mensaje.isEmpty()) {
                Toast.makeText(this, "Completá nombre, teléfono y mensaje", Toast.LENGTH_SHORT).show();
                return;
            }
            int minutos = getMinutosSeleccionados();
            api.crear(nombre, telefono, mensaje, minutos, (ok, msg) -> {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                if (ok) {
                    editNombre.setText("");
                    editTelefono.setText("");
                    editMensaje.setText("");
                    spinnerMinutos.setSelection(0);
                    cargarPendientes();
                }
            });
        });

        // ③ wa.me directo (sin BD)
        botonWaMeForm.setOnClickListener(v -> {
            String telefono = editTelefono.getText().toString().trim();
            String mensaje = editMensaje.getText().toString().trim();
            if (telefono.isEmpty() || mensaje.isEmpty()) {
                Toast.makeText(this, "Completá teléfono y mensaje", Toast.LENGTH_SHORT).show();
                return;
            }
            abrirWhatsApp(telefono, mensaje);
        });

        // ① Enviar pendientes (servidor/OpenWA)
        botonPendientes.setOnClickListener(v ->
                api.dispatch((ok, mensaje) ->
                        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()));

        // ④ wa.me desde BD (primer pendiente)
        botonWaMe.setOnClickListener(v ->
                api.obtenerPendientes((lista, error) -> {
                    if (error != null || lista == null || lista.isEmpty()) {
                        Toast.makeText(this, "No hay pendientes", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Recordatorio r = lista.get(0);
                    abrirWhatsApp(r.telefono, r.mensaje);
                }));

        // ⑤ Reset gate lazy sweep
        botonResetGate.setOnClickListener(v ->
                api.resetGate((ok, msg) -> {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    cargarPendientes();
                }));

        cargarPendientes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarPendientes();
    }

    private int getMinutosSeleccionados() {
        int pos = spinnerMinutos.getSelectedItemPosition();
        switch (pos) {
            case 1: return 1;
            case 2: return 5;
            case 3: return 10;
            case 4: return 15;
            case 5: return 30;
            case 6: return 60;
            default: return 0;
        }
    }

    private void cargarPendientes() {
        api.obtenerPendientes((lista, error) -> {
            if (error != null) {
                textoPendientes.setText("Error: " + error);
                return;
            }
            int n = (lista == null) ? 0 : lista.size();
            textoPendientes.setText("Pendientes: " + n);
        });
    }

    private void abrirWhatsApp(String telefono, String mensaje) {
        try {
            String numero = normalizarNumero(telefono);
            String url = "https://wa.me/" + numero + "?text=" + Uri.encode(mensaje);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_LONG).show();
        }
    }

    private String normalizarNumero(String telefono) {
        String soloDigitos = telefono == null ? "" : telefono.replaceAll("[^0-9]", "");
        if (soloDigitos.length() >= 10 && soloDigitos.length() <= 12 && !soloDigitos.startsWith("54")) {
            return "54" + soloDigitos;
        }
        return soloDigitos;
    }
}