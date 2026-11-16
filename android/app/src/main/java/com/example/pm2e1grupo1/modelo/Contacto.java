package com.example.pm2e1grupo1.modelo;

/**
 * MODELO: contacto tal como lo devuelve la API (se convierte desde JSON con Gson).
 * Los nombres de los campos coinciden con el JSON del servidor.
 */
public class Contacto {

    private int id;
    private String nombre;
    private String telefono;
    private double latitud;
    private double longitud;
    private String tipoMime;
    private long tamanoBytes;
    private String archivoUrl;
    private String fechaCreacion;

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    /** Ruta relativa de la fotografia, por ejemplo /api/contactos/5/archivo. */
    public String getArchivoUrl() {
        return archivoUrl;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }
}
