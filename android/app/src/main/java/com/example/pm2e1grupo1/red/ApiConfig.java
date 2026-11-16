package com.example.pm2e1grupo1.red;

/** Configuracion de la API REST. */
public final class ApiConfig {

    /**
     * URL base de la API.
     * - Emulador de Android Studio: 10.0.2.2 apunta al localhost de la PC.
     * - Telefono fisico: cambiar por la IP de la PC en la red, por ejemplo http://192.168.1.10:5000
     */
    public static final String BASE_URL = "http://10.0.2.2:5000";

    public static final String RUTA_CONTACTOS = BASE_URL + "/api/contactos";

    /** MIME type que acepta la API y tamano maximo (5 MB). */
    public static final String MIME_JPEG = "image/jpeg";
    public static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private ApiConfig() {
    }
}
