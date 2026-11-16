package com.example.pm2e1grupo1.controlador;

import java.util.regex.Pattern;

/**
 * CONTROLADOR: reglas de negocio del formulario de contacto.
 * La vista le pide validar los datos y muestra el primer error que encuentre.
 */
public class ContactoControlador {

    /** Primer error encontrado al validar el formulario. */
    public static class Error {
        public enum Campo {FOTO, NOMBRE, TELEFONO}

        public final Campo campo;
        public final String titulo;
        public final String mensaje;
        public final String errorCampo;

        Error(Campo campo, String titulo, String mensaje, String errorCampo) {
            this.campo = campo;
            this.titulo = titulo;
            this.mensaje = mensaje;
            this.errorCampo = errorCampo;
        }
    }

    private static final Pattern PATRON_NOMBRE = Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$");
    private static final Pattern PATRON_TELEFONO = Pattern.compile("^\\d{8}$");

    /** Devuelve el primer error del formulario, o null si todos los datos son validos. */
    public Error validar(boolean hayFotografia, String nombre, String telefono) {
        if (!hayFotografia) {
            return new Error(Error.Campo.FOTO, "Falta la fotografía",
                    "No se ha tomado fotografía.", null);
        }
        if (nombre.isEmpty()) {
            return new Error(Error.Campo.NOMBRE, "Falta el nombre",
                    "Debe escribir un nombre.", "Campo obligatorio");
        }
        if (!PATRON_NOMBRE.matcher(nombre).matches()) {
            return new Error(Error.Campo.NOMBRE, "Nombre no válido",
                    "El nombre solo debe contener letras y espacios.", "Solo letras y espacios");
        }
        if (telefono.isEmpty()) {
            return new Error(Error.Campo.TELEFONO, "Falta el teléfono",
                    "Debe escribir un teléfono de 8 dígitos.", "Campo obligatorio");
        }
        if (!PATRON_TELEFONO.matcher(telefono).matches()) {
            return new Error(Error.Campo.TELEFONO, "Teléfono no válido",
                    "El teléfono debe tener exactamente 8 dígitos numéricos.", "Deben ser 8 dígitos");
        }
        return null;
    }
}
