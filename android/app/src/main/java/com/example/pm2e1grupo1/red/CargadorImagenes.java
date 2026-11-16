package com.example.pm2e1grupo1.red;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import com.example.pm2e1grupo1.R;

/**
 * Descarga las fotografias desde la API (con OkHttp) y las muestra en un ImageView.
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

    /**
     * Muestra la fotografia en la imagen. Mientras llega (o si falla) se ve la silueta.
     * ladoMaximo: tamano en pixeles al que se reduce la foto para no gastar memoria.
     */
    public static void mostrar(ImageView imagen, String archivoUrl, int ladoMaximo) {
        String clave = archivoUrl + "@" + ladoMaximo;
        imagen.setTag(R.id.imgFoto, clave); // marca qué foto espera esta vista (las filas se reciclan)

        Bitmap guardada = CACHE.get(clave);
        if (guardada != null) {
            imagen.setImageBitmap(guardada);
            return;
        }

        imagen.setImageResource(R.drawable.ic_persona);
        ContactosApi.descargarFotografia(archivoUrl, new ContactosApi.Respuesta<byte[]>() {
            @Override
            public void alExito(byte[] bytes) {
                Bitmap decodificada = decodificar(bytes, ladoMaximo);
                if (decodificada == null) {
                    return;
                }
                CACHE.put(clave, decodificada);
                HILO_PRINCIPAL.post(() -> {
                    // Solo se pinta si la vista sigue esperando esta misma foto.
                    if (clave.equals(imagen.getTag(R.id.imgFoto))) {
                        imagen.setImageBitmap(decodificada);
                    }
                });
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
