package com.example.pm2e1grupo1.vista

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pm2e1grupo1.R
import com.example.pm2e1grupo1.controlador.ContactoControlador
import com.example.pm2e1grupo1.modelo.Contacto
import com.example.pm2e1grupo1.red.ApiConfig
import com.example.pm2e1grupo1.red.CargadorImagenes
import com.example.pm2e1grupo1.red.ContactosApi
import com.example.pm2e1grupo1.red.ErrorApi
import com.example.pm2e1grupo1.ui.AlertaUi
import com.example.pm2e1grupo1.ui.EditarEstado
import com.example.pm2e1grupo1.ui.PantallaEditar
import com.example.pm2e1grupo1.ui.TemaApp
import com.example.pm2e1grupo1.ui.TipoAlerta
import com.example.pm2e1grupo1.util.CapturaFoto
import com.example.pm2e1grupo1.util.ImagenJpeg
import okhttp3.Call
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Actualizar un contacto (PUT), en Jetpack Compose. Muestra los datos actuales; la fotografia es
 * opcional: si no se toma una nueva, se conserva la que ya tiene el contacto en la API.
 */
class EditarContactoActivity : ComponentActivity() {

    private lateinit var estado: EditarEstado
    private val controlador = ContactoControlador()
    private var id = -1

    private val capturaFoto = CapturaFoto(this, { estado.fotoNueva = it }, { estado.alerta = it })

    private val trabajo = Executors.newSingleThreadExecutor()
    private var llamadaActual: Call? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        id = intent.getIntExtra(EXTRA_ID, -1)
        estado = EditarEstado(
            nombre = intent.getStringExtra(EXTRA_NOMBRE).orEmpty(),
            telefono = intent.getStringExtra(EXTRA_TELEFONO).orEmpty(),
            latitud = intent.getDoubleExtra(EXTRA_LATITUD, 0.0),
            longitud = intent.getDoubleExtra(EXTRA_LONGITUD, 0.0),
            archivoUrl = intent.getStringExtra(EXTRA_ARCHIVO_URL).orEmpty(),
        )

        setContent {
            TemaApp {
                PantallaEditar(
                    estado = estado,
                    alAtras = ::finish,
                    alTomarFoto = capturaFoto::tomar,
                    alGuardar = ::guardar,
                )
            }
        }
    }

    override fun onDestroy() {
        llamadaActual?.cancel()
        trabajo.shutdownNow()
        super.onDestroy()
    }

    // ---------- Guardar (PUT) ----------

    private fun guardar() {
        val nombre = estado.nombre.trim()
        val telefono = estado.telefono.trim()

        // La fotografia no es obligatoria al actualizar: el contacto ya tiene una.
        val error = controlador.validar(true, nombre, telefono)
        if (error != null) {
            when (error.campo) {
                ContactoControlador.Error.Campo.NOMBRE -> estado.errorNombre = error.errorCampo
                ContactoControlador.Error.Campo.TELEFONO -> estado.errorTelefono = error.errorCampo
                ContactoControlador.Error.Campo.FOTO -> Unit
            }
            val tipo = if (error.titulo.startsWith("Falta")) TipoAlerta.ADVERTENCIA else TipoAlerta.ERROR
            estado.alerta = AlertaUi(tipo, error.titulo, error.mensaje)
            return
        }
        enviar(nombre, telefono)
    }

    private fun enviar(nombre: String, telefono: String) {
        estado.cargando = true
        val foto = estado.fotoNueva

        trabajo.execute {
            val aSubir = try {
                foto?.let { ImagenJpeg.ajustarAlLimite(it, ApiConfig.TAMANO_MAXIMO_BYTES) }
            } catch (ex: IOException) {
                runOnUiThread {
                    estado.cargando = false
                    estado.alerta = AlertaUi(
                        TipoAlerta.ERROR, "Fotografía no válida", ex.message ?: "No se pudo leer la fotografía.",
                    )
                }
                return@execute
            }

            llamadaActual = ContactosApi.actualizar(
                id, nombre, telefono, estado.latitud, estado.longitud, aSubir,
                object : ContactosApi.Respuesta<Contacto> {
                    override fun alExito(resultado: Contacto) {
                        runOnUiThread { alActualizar() }
                    }

                    override fun alError(error: ErrorApi) {
                        runOnUiThread { alErrorDeApi(error, nombre, telefono) }
                    }
                },
            )
        }
    }

    private fun alActualizar() {
        if (isFinishing || isDestroyed) return
        CargadorImagenes.limpiarCache() // la foto pudo cambiar: la lista debe volver a descargarla
        setResult(RESULT_OK)
        finish()
    }

    private fun alErrorDeApi(error: ErrorApi, nombre: String, telefono: String) {
        if (isFinishing || isDestroyed) return
        estado.cargando = false

        estado.alerta = if (error.esSinConexion()) {
            AlertaUi(
                tipo = TipoAlerta.ERROR,
                titulo = "Sin conexión",
                mensaje = error.mensaje,
                textoAceptar = getString(R.string.reintentar),
                textoCancelar = getString(R.string.cancelar),
                alAceptar = { enviar(nombre, telefono) },
            )
        } else {
            AlertaUi(TipoAlerta.ERROR, "No se pudo actualizar", error.mensaje)
        }
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_NOMBRE = "nombre"
        const val EXTRA_TELEFONO = "telefono"
        const val EXTRA_LATITUD = "latitud"
        const val EXTRA_LONGITUD = "longitud"
        const val EXTRA_ARCHIVO_URL = "archivoUrl"
    }
}
