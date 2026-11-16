package com.example.pm2e1grupo1.red;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pm2e1grupo1.modelo.Contacto;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
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

    /** Convierte el cuerpo de una respuesta exitosa en el objeto que espera la vista. */
    private interface Convertidor<T> {
        T convertir(ResponseBody cuerpo) throws IOException;
    }

    /** Forma del JSON de error del servidor: { "mensaje": "...", "errores": [...] }. */
    private static class ErrorJson {
        String mensaje;
    }

    private static final Gson GSON = new Gson();

    private ContactosApi() {
    }

    /** GET /api/contactos?buscar= : lista los contactos, filtrados por nombre o telefono si hay texto. */
    public static Call listar(@Nullable String buscar, Respuesta<List<Contacto>> respuesta) {
        HttpUrl.Builder url = HttpUrl.get(ApiConfig.RUTA_CONTACTOS).newBuilder();
        if (buscar != null && !buscar.trim().isEmpty()) {
            url.addQueryParameter("buscar", buscar.trim());
        }

        Request peticion = new Request.Builder().url(url.build()).get().build();
        return ejecutar(peticion,
                cuerpo -> Arrays.asList(GSON.fromJson(cuerpo.string(), Contacto[].class)),
                respuesta);
    }

    /** POST /api/contactos (multipart/form-data) con la fotografia como image/jpeg. */
    public static Call crear(String nombre, String telefono, double latitud, double longitud,
                             File fotografia, Respuesta<Contacto> respuesta) {
        Request peticion = new Request.Builder()
                .url(ApiConfig.RUTA_CONTACTOS)
                .post(formulario(nombre, telefono, latitud, longitud, fotografia))
                .build();
        return ejecutar(peticion, cuerpo -> GSON.fromJson(cuerpo.string(), Contacto.class), respuesta);
    }

    /** PUT /api/contactos/{id} (multipart/form-data). La fotografia es opcional: null conserva la actual. */
    public static Call actualizar(int id, String nombre, String telefono, double latitud, double longitud,
                                  @Nullable File fotografia, Respuesta<Contacto> respuesta) {
        Request peticion = new Request.Builder()
                .url(ApiConfig.RUTA_CONTACTOS + "/" + id)
                .put(formulario(nombre, telefono, latitud, longitud, fotografia))
                .build();
        return ejecutar(peticion, cuerpo -> GSON.fromJson(cuerpo.string(), Contacto.class), respuesta);
    }

    /** DELETE /api/contactos/{id} : la API responde 204 sin cuerpo. */
    public static Call eliminar(int id, Respuesta<Void> respuesta) {
        Request peticion = new Request.Builder()
                .url(ApiConfig.RUTA_CONTACTOS + "/" + id)
                .delete()
                .build();
        return ejecutar(peticion, cuerpo -> null, respuesta);
    }

    /** GET de la fotografia: archivoUrl es la ruta relativa del contacto (/api/contactos/{id}/archivo). */
    public static Call descargarFotografia(String archivoUrl, Respuesta<byte[]> respuesta) {
        Request peticion = new Request.Builder().url(ApiConfig.BASE_URL + archivoUrl).get().build();
        return ejecutar(peticion, ResponseBody::bytes, respuesta);
    }

    private static RequestBody formulario(String nombre, String telefono, double latitud, double longitud,
                                          @Nullable File fotografia) {
        MultipartBody.Builder cuerpo = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("nombre", nombre)
                .addFormDataPart("telefono", telefono)
                .addFormDataPart("latitud", numero(latitud))
                .addFormDataPart("longitud", numero(longitud));

        if (fotografia != null) {
            RequestBody archivo = RequestBody.create(fotografia, MediaType.parse(ApiConfig.MIME_JPEG));
            cuerpo.addFormDataPart("archivo", fotografia.getName(), archivo);
        }
        return cuerpo.build();
    }

    /** Siempre con punto decimal, sin importar el idioma del telefono (el servidor espera 14.48). */
    private static String numero(double valor) {
        return String.format(Locale.US, "%.6f", valor);
    }

    private static <T> Call ejecutar(Request peticion, Convertidor<T> convertidor, Respuesta<T> respuesta) {
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
                    if (response.isSuccessful()) {
                        respuesta.alExito(convertidor.convertir(cuerpo));
                    } else {
                        String texto = cuerpo == null ? "" : cuerpo.string();
                        respuesta.alError(ErrorApi.deServidor(response.code(), mensajeDeError(texto, response.code())));
                    }
                } catch (IOException e) {
                    if (!call.isCanceled()) {
                        respuesta.alError(ErrorApi.deConexion());
                    }
                } catch (JsonSyntaxException | NullPointerException e) {
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
