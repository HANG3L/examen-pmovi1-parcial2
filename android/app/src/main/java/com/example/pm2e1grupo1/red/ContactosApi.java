package com.example.pm2e1grupo1.red;

import androidx.annotation.NonNull;

import com.example.pm2e1grupo1.modelo.Contacto;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Peticiones a la API de contactos con OkHttp.
 * Todas usan enqueue(): se ejecutan en un hilo de OkHttp, nunca en el hilo principal.
 * Los callbacks llegan en ese mismo hilo, por eso la vista debe usar runOnUiThread().
 */
public final class ContactosApi {

    /** Resultado de una peticion: exito con dato, o error. */
    public interface Respuesta<T> {
        void alExito(T resultado);

        void alError(ErrorApi error);
    }

    /** Forma del JSON de error del servidor: { "mensaje": "...", "errores": [...] }. */
    private static class ErrorJson {
        String mensaje;
    }

    private static final Gson GSON = new Gson();

    private ContactosApi() {
    }

    /** POST /api/contactos (multipart/form-data) con la fotografia como image/jpeg. */
    public static Call crear(String nombre, String telefono, double latitud, double longitud,
                             File fotografia, Respuesta<Contacto> respuesta) {
        RequestBody cuerpoArchivo = RequestBody.create(fotografia, MediaType.parse(ApiConfig.MIME_JPEG));

        RequestBody cuerpo = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("nombre", nombre)
                .addFormDataPart("telefono", telefono)
                .addFormDataPart("latitud", numero(latitud))
                .addFormDataPart("longitud", numero(longitud))
                .addFormDataPart("archivo", fotografia.getName(), cuerpoArchivo)
                .build();

        Request peticion = new Request.Builder()
                .url(ApiConfig.RUTA_CONTACTOS)
                .post(cuerpo)
                .build();

        return ejecutar(peticion, Contacto.class, respuesta);
    }

    /** Siempre con punto decimal, sin importar el idioma del telefono (el servidor espera 14.48). */
    private static String numero(double valor) {
        return String.format(Locale.US, "%.6f", valor);
    }

    private static <T> Call ejecutar(Request peticion, Class<T> tipo, Respuesta<T> respuesta) {
        Call llamada = ClienteHttp.obtener().newCall(peticion);
        llamada.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (!call.isCanceled()) {
                    respuesta.alError(ErrorApi.deConexion());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    ResponseBody cuerpo = response.body();
                    String texto = cuerpo == null ? "" : cuerpo.string();

                    if (response.isSuccessful()) {
                        respuesta.alExito(GSON.fromJson(texto, tipo));
                    } else {
                        respuesta.alError(ErrorApi.deServidor(response.code(), mensajeDeError(texto, response.code())));
                    }
                } catch (IOException e) {
                    respuesta.alError(ErrorApi.deConexion());
                } catch (JsonSyntaxException e) {
                    respuesta.alError(ErrorApi.deServidor(0, "La respuesta del servidor no es válida."));
                }
            }
        });
        return llamada;
    }

    private static String mensajeDeError(String json, int codigo) {
        try {
            ErrorJson error = GSON.fromJson(json, ErrorJson.class);
            if (error != null && error.mensaje != null && !error.mensaje.isEmpty()) {
                return error.mensaje;
            }
        } catch (JsonSyntaxException ignorado) {
            // la respuesta no era JSON: se usa el mensaje generico
        }
        return "El servidor respondió con un error (" + codigo + ").";
    }
}
