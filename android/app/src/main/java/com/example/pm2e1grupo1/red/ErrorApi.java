package com.example.pm2e1grupo1.red;

/** Error de una peticion a la API: falta de conexion o respuesta de error del servidor. */
public class ErrorApi {

    private final boolean sinConexion;
    private final int codigo;
    private final String mensaje;

    private ErrorApi(boolean sinConexion, int codigo, String mensaje) {
        this.sinConexion = sinConexion;
        this.codigo = codigo;
        this.mensaje = mensaje;
    }

    /** No se pudo llegar al servidor (sin red, servidor apagado, tiempo agotado). */
    public static ErrorApi deConexion() {
        return new ErrorApi(true, 0, "Sin conexión con la API. Intente de nuevo.");
    }

    /** El servidor respondio con un error (400, 404, 415, 500...). */
    public static ErrorApi deServidor(int codigo, String mensaje) {
        return new ErrorApi(false, codigo, mensaje);
    }

    public boolean esSinConexion() {
        return sinConexion;
    }

    public int getCodigo() {
        return codigo;
    }

    /** Mensaje listo para mostrar al usuario (en el JSON del servidor es el campo "mensaje"). */
    public String getMensaje() {
        return mensaje;
    }
}
