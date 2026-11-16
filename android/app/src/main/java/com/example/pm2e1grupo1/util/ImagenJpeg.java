package com.example.pm2e1grupo1.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Prepara la fotografia para subirla: la API solo acepta JPEG de hasta 5 MB.
 * Las camaras modernas pueden superar ese tamano, asi que se reduce solo cuando hace falta.
 */
public final class ImagenJpeg {

    private static final int LADO_MAXIMO = 2048;

    private ImagenJpeg() {
    }

    /**
     * Devuelve el mismo archivo si ya cabe en el limite; si no, crea una copia JPEG mas liviana
     * (respetando la orientacion de la foto). Se debe llamar fuera del hilo principal.
     */
    public static File ajustarAlLimite(File original, long maximoBytes) throws IOException {
        if (original.length() <= maximoBytes) {
            return original;
        }

        BitmapFactory.Options medidas = new BitmapFactory.Options();
        medidas.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(original.getAbsolutePath(), medidas);

        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = factorDeReduccion(medidas.outWidth, medidas.outHeight);
        Bitmap imagen = BitmapFactory.decodeFile(original.getAbsolutePath(), opciones);
        if (imagen == null) {
            throw new IOException("No se pudo leer la fotografía.");
        }

        imagen = rotarSegunExif(imagen, original);

        File reducida = new File(original.getParentFile(), "UP_" + original.getName());
        int calidad = 90;
        do {
            try (FileOutputStream salida = new FileOutputStream(reducida)) {
                imagen.compress(Bitmap.CompressFormat.JPEG, calidad, salida);
            }
            calidad -= 10;
        } while (reducida.length() > maximoBytes && calidad >= 40);

        imagen.recycle();

        if (reducida.length() > maximoBytes) {
            throw new IOException("La fotografía es demasiado grande.");
        }
        return reducida;
    }

    /**
     * Version reducida y bien orientada de la foto para mostrarla en pantalla.
     * Se debe llamar fuera del hilo principal; devuelve null si no se puede leer.
     */
    public static Bitmap cargarMiniatura(File archivo, int ladoMaximo) {
        BitmapFactory.Options medidas = new BitmapFactory.Options();
        medidas.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(archivo.getAbsolutePath(), medidas);

        int factor = 1;
        while (Math.max(medidas.outWidth, medidas.outHeight) / (factor * 2) >= ladoMaximo) {
            factor *= 2;
        }

        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = factor;
        Bitmap imagen = BitmapFactory.decodeFile(archivo.getAbsolutePath(), opciones);
        return imagen == null ? null : rotarSegunExif(imagen, archivo);
    }

    private static int factorDeReduccion(int ancho, int alto) {
        int factor = 1;
        while (Math.max(ancho, alto) / factor > LADO_MAXIMO) {
            factor *= 2;
        }
        return factor;
    }

    private static Bitmap rotarSegunExif(Bitmap imagen, File archivo) {
        try {
            int orientacion = new ExifInterface(archivo.getAbsolutePath())
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int grados;
            switch (orientacion) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    grados = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    grados = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    grados = 270;
                    break;
                default:
                    return imagen;
            }
            Matrix matriz = new Matrix();
            matriz.postRotate(grados);
            Bitmap rotada = Bitmap.createBitmap(imagen, 0, 0, imagen.getWidth(), imagen.getHeight(), matriz, true);
            if (rotada != imagen) {
                imagen.recycle();
            }
            return rotada;
        } catch (IOException e) {
            return imagen;
        }
    }
}
