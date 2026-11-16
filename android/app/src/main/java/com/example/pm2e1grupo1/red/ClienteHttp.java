package com.example.pm2e1grupo1.red;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/** Unico OkHttpClient de toda la app (singleton): comparte conexiones, cache y configuracion. */
public final class ClienteHttp {

    private static volatile OkHttpClient instancia;

    private ClienteHttp() {
    }

    public static OkHttpClient obtener() {
        if (instancia == null) {
            synchronized (ClienteHttp.class) {
                if (instancia == null) {
                    // BASIC registra metodo, URL y codigo, sin volcar los bytes de la fotografia.
                    HttpLoggingInterceptor registro = new HttpLoggingInterceptor();
                    registro.setLevel(HttpLoggingInterceptor.Level.BASIC);

                    instancia = new OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .addInterceptor(registro)
                            .build();
                }
            }
        }
        return instancia;
    }
}
