package com.tuapp;

/**
 * Modelo: representa una fila de la tabla "recordatorios".
 * Es solo un contenedor de datos, sin lógica.
 */
public class Recordatorio {
    public int id;
    public String nombre;
    public String telefono;
    public String mensaje;
    public String programadoPara;

    public Recordatorio(int id, String nombre, String telefono, String mensaje, String programadoPara) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
        this.mensaje = mensaje;
        this.programadoPara = programadoPara;
    }

    public Recordatorio(int id, String nombre, String telefono, String mensaje) {
        this(id, nombre, telefono, mensaje, null);
    }
}
