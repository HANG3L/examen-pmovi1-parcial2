package com.example.pm2e1grupo1.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.pm2e1grupo1.ui.AlertaUi
import com.example.pm2e1grupo1.ui.TipoAlerta
import com.example.pm2e1grupo1.ui.alertaPermisoDenegado
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Toma una fotografia con la camara: pide el permiso, abre la camara con TakePicture + FileProvider
 * y entrega el archivo JPEG resultante. Se debe crear antes de que la actividad llegue a STARTED
 * (como propiedad de la actividad o en onCreate).
 */
class CapturaFoto(
    private val actividad: ComponentActivity,
    private val alTomar: (File) -> Unit,
    private val alAlertar: (AlertaUi) -> Unit,
) {

    /** Archivo donde la camara esta escribiendo; solo vale si la camara confirma la foto. */
    private var archivoPendiente: File? = null

    private val lanzadorPermiso =
        actividad.registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (concedido) {
                abrirCamara()
            } else {
                alAlertar(
                    alertaPermisoDenegado(
                        actividad, Manifest.permission.CAMERA, "Permiso de cámara denegado",
                        "Sin acceso a la cámara no es posible tomar la fotografía del contacto.",
                    ),
                )
            }
        }

    private val lanzadorCamara =
        actividad.registerForActivityResult(ActivityResultContracts.TakePicture()) { tomada ->
            val archivo = archivoPendiente
            archivoPendiente = null
            if (archivo != null) {
                if (tomada && archivo.length() > 0) alTomar(archivo) else archivo.delete()
            }
        }

    /** Punto de entrada: comprueba el permiso y abre la camara. */
    fun tomar() {
        val concedido = ContextCompat.checkSelfPermission(actividad, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (concedido) abrirCamara() else lanzadorPermiso.launch(Manifest.permission.CAMERA)
    }

    private fun abrirCamara() {
        val archivo = try {
            crearArchivoImagen()
        } catch (ex: IOException) {
            alAlertar(
                AlertaUi(
                    TipoAlerta.ERROR, "Error",
                    "No se pudo crear el archivo para la fotografía. Intenta de nuevo.",
                ),
            )
            return
        }
        archivoPendiente = archivo
        lanzadorCamara.launch(
            FileProvider.getUriForFile(actividad, "${actividad.packageName}.fileprovider", archivo),
        )
    }

    private fun crearArchivoImagen(): File {
        val fecha = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val carpeta = actividad.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${fecha}_", ".jpg", carpeta)
    }
}
