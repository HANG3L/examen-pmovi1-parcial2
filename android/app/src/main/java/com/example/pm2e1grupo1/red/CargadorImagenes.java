package com.example.pm2e1grupo1.red;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import java.util.function.Consumer;

/**
 * Descarga las fotografias desde la API (con OkHttp) y las entrega ya reducidas.
 * Guarda las ya descargadas en memoria para que la lista no vuelva a pedirlas al hacer scroll.
 */
public final class CargadorImagenes {

    private static final Handler HILO_PRINCIPAL = new Handler(Looper.getMainLooper());

    // Cache: usa 1/8 de la memoria disponible, medido en KB.
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 1024 / 8)) {
                @Override
                protected int sizeOf(String clave, Bitmap imagen) {
                    return imagen.getByteCount() / 1024;
                }
            };

    private CargadorImagenes() {
    }

    private static String clave(String archivoUrl, int ladoMaximo) {
        return archivoUrl + "@" + ladoMaximo;
    }

    /** Foto ya descargada, o null si todavia no esta en memoria. */
    public static Bitmap enCache(String archivoUrl, int ladoMaximo) {
        return CACHE.get(clave(archivoUrl, ladoMaximo));
    }

    /**
     * Descarga la fotografia y la entrega en el hilo principal.
     * ladoMaximo: tamano en pixeles al que se reduce la foto para no gastar memoria.
     * Si falla la descarga no se llama al oyente (la pantalla se queda con la silueta).
     */
    public static void cargar(String archivoUrl, int ladoMaximo, Consumer<Bitmap> alCargar) {
        String clave = clave(archivoUrl, ladoMaximo);

        Bitmap guardada = CACHE.get(clave);
        if (guardada != null) {
            alCargar.accept(guardada);
            return;
        }

        ContactosApi.descargarFotografia(archivoUrl, new ContactosApi.Respuesta<byte[]>() {
            @Override
            public void alExito(byte[] bytes) {
                Bitmap decodificada = decodificar(bytes, ladoMaximo);
                if (decodificada == null) {
                    return;
                }
                CACHE.put(clave, decodificada);
                HILO_PRINCIPAL.post(() -> alCargar.accept(decodificada));
            }

            @Override
            public void alError(ErrorApi error) {
                // se queda la silueta
            }
        });
    }

    /** Descarta las fotos guardadas (por ejemplo, despues de cambiar la foto de un contacto). */
    public static void limpiarCache() {
        CACHE.evictAll();
    }

    private static Bitmap decodificar(byte[] bytes, int ladoMaximo) {
        BitmapFactory.Options medidas = new BitmapFactory.Options();
        medidas.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, medidas);

        int factor = 1;
        while (Math.max(medidas.outWidth, medidas.outHeight) / (factor * 2) >= ladoMaximo) {
            factor *= 2;
        }

        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = factor;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opciones);
    }
}
