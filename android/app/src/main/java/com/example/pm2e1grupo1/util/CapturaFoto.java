package com.example.pm2e1grupo1.util;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Toma una fotografia con la camara: pide el permiso, abre la camara con TakePicture + FileProvider
 * y entrega el archivo JPEG resultante. Se crea en onCreate de la actividad.
 */
public class CapturaFoto {

    private final AppCompatActivity actividad;
    private final Consumer<File> alTomar;

    /** Archivo donde la camara esta escribiendo; solo vale si la camara confirma la foto. */
    private File archivoPendiente;

    private final ActivityResultLauncher<String> lanzadorPermiso;
    private final ActivityResultLauncher<Uri> lanzadorCamara;

    public CapturaFoto(AppCompatActivity actividad, Consumer<File> alTomar) {
        this.actividad = actividad;
        this.alTomar = alTomar;

        lanzadorPermiso = actividad.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), concedido -> {
                    if (concedido) {
                        abrirCamara();
                    } else {
                        Permisos.avisarDenegado(actividad, Manifest.permission.CAMERA,
                                "Permiso de cámara denegado",
                                "Sin acceso a la cámara no es posible tomar la fotografía del contacto.");
                    }
                });

        lanzadorCamara = actividad.registerForActivityResult(
                new ActivityResultContracts.TakePicture(), tomada -> {
                    File archivo = archivoPendiente;
                    archivoPendiente = null;
                    if (archivo == null) {
                        return;
                    }
                    if (tomada && archivo.length() > 0) {
                        alTomar.accept(archivo);
                    } else {
                        archivo.delete();
                    }
                });
    }

    /** Punto de entrada: comprueba el permiso y abre la camara. */
    public void tomar() {
        if (ContextCompat.checkSelfPermission(actividad, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            lanzadorPermiso.launch(Manifest.permission.CAMERA);
        }
    }

    /** URI (content://) para mostrar la foto en un ImageView. */
    public Uri uriDe(File archivo) {
        return FileProvider.getUriForFile(actividad, actividad.getPackageName() + ".fileprovider", archivo);
    }

    private void abrirCamara() {
        try {
            archivoPendiente = crearArchivoImagen();
        } catch (IOException ex) {
            Alertas.mostrar(actividad, Alertas.Tipo.ERROR, "Error",
                    "No se pudo crear el archivo para la fotografía. Intenta de nuevo.");
            return;
        }
        lanzadorCamara.launch(uriDe(archivoPendiente));
    }

    private File crearArchivoImagen() throws IOException {
        String fecha = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File carpeta = actividad.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + fecha + "_", ".jpg", carpeta);
    }
}
