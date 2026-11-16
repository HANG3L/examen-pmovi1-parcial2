package com.example.pm2e1grupo1.vista

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.pm2e1grupo1.R
import com.example.pm2e1grupo1.controlador.ContactoControlador
import com.example.pm2e1grupo1.modelo.Contacto
import com.example.pm2e1grupo1.red.ApiConfig
import com.example.pm2e1grupo1.red.ContactosApi
import com.example.pm2e1grupo1.red.ErrorApi
import com.example.pm2e1grupo1.ui.AlertaUi
import com.example.pm2e1grupo1.ui.AvisoUi
import com.example.pm2e1grupo1.ui.PantallaRegistro
import com.example.pm2e1grupo1.ui.RegistroEstado
import com.example.pm2e1grupo1.ui.TemaApp
import com.example.pm2e1grupo1.ui.TipoAlerta
import com.example.pm2e1grupo1.ui.alertaPermisoDenegado
import com.example.pm2e1grupo1.util.CapturaFoto
import com.example.pm2e1grupo1.util.GestorUbicacion
import com.example.pm2e1grupo1.util.ImagenJpeg
import com.example.pm2e1grupo1.util.RedLocal
import com.example.pm2e1grupo1.ui.alertaRedLocalDenegada
import okhttp3.Call
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/** Pantalla 1 (Jetpack Compose): registro de un contacto con fotografia y ubicacion, enviado a la API. */
class MainActivity : ComponentActivity() {

    private val estado = RegistroEstado()
    private val controlador = ContactoControlador()

    private val capturaFoto = CapturaFoto(this, { estado.foto = it }, { estado.alerta = it })
    private lateinit var gestorUbicacion: GestorUbicacion

    private var permisosSolicitados = false
    private var avisoGpsMostrado = false

    /** Un solo hilo de trabajo: ajustar la foto no debe correr en el hilo principal. */
    private val trabajo = Executors.newSingleThreadExecutor()
    private var llamadaActual: Call? = null

    /** Ubicacion y red local (Android 17+) se piden juntas al abrir la pantalla. */
    private val lanzadorPermisos =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            when {
                !gestorUbicacion.tienePermiso() -> estado.alerta = alertaUbicacionDenegada()
                !RedLocal.concedido(this) -> estado.alerta = alertaRedLocalDenegada(this)
                else -> iniciarUbicacion()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        gestorUbicacion = GestorUbicacion(this) { latitud, longitud ->
            estado.latitud = latitud
            estado.longitud = longitud
        }

        setContent {
            TemaApp {
                PantallaRegistro(
                    estado = estado,
                    alTomarFoto = capturaFoto::tomar,
                    alGuardar = ::guardar,
                    alVerContactos = {
                        startActivity(Intent(this, ListaContactosActivity::class.java))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        iniciarUbicacion()
    }

    override fun onPause() {
        gestorUbicacion.detener()
        super.onPause()
    }

    override fun onDestroy() {
        llamadaActual?.cancel()
        trabajo.shutdownNow()
        super.onDestroy()
    }

    // ---------- Ubicacion ----------

    /** Arranca la ubicacion en tiempo real, o explica por que no se puede (permiso o GPS apagado). */
    private fun iniciarUbicacion() {
        if (pedirPermisosPendientes() || !gestorUbicacion.tienePermiso()) return
        if (!gestorUbicacion.gpsActivo()) {
            if (!avisoGpsMostrado) {
                avisoGpsMostrado = true
                estado.alerta = alertaGpsApagado()
            }
            return
        }
        avisoGpsMostrado = false
        gestorUbicacion.iniciar()
    }

    /**
     * Pide (una sola vez por pantalla) los permisos que falten: ubicacion y, en Android 17+,
     * red local para poder llegar a la API. Devuelve true si se abrio la solicitud.
     */
    private fun pedirPermisosPendientes(): Boolean {
        if (permisosSolicitados) return false

        val pendientes = buildList {
            if (!gestorUbicacion.tienePermiso()) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (!RedLocal.concedido(this@MainActivity)) add(RedLocal.PERMISO)
        }
        if (pendientes.isEmpty()) return false

        permisosSolicitados = true
        lanzadorPermisos.launch(pendientes.toTypedArray())
        return true
    }

    private fun alertaGpsApagado() = AlertaUi(
        tipo = TipoAlerta.ADVERTENCIA,
        titulo = "Alerta",
        mensaje = "Gps no está activo.",
        textoAceptar = getString(R.string.activar_gps),
        textoCancelar = getString(R.string.cancelar),
        alAceptar = { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
    )

    private fun alertaUbicacionDenegada() = alertaPermisoDenegado(
        this, Manifest.permission.ACCESS_FINE_LOCATION, "Permiso de ubicación denegado",
        "Sin acceso a la ubicación no es posible guardar el contacto.",
    )

    /** true si ya hay una ubicacion valida; si no, muestra la alerta que corresponda. */
    private fun ubicacionLista(): Boolean {
        if (!gestorUbicacion.tienePermiso()) {
            estado.alerta = alertaUbicacionDenegada()
            return false
        }
        if (!gestorUbicacion.gpsActivo()) {
            estado.alerta = alertaGpsApagado()
            return false
        }
        if (estado.latitud == null || estado.longitud == null) {
            estado.alerta = AlertaUi(
                TipoAlerta.ADVERTENCIA, "Ubicación requerida",
                "Debe describir la ubicación. Espera a que el GPS la obtenga.",
            )
            return false
        }
        return true
    }

    // ---------- Guardar ----------

    private fun guardar() {
        val nombre = estado.nombre.trim()
        val telefono = estado.telefono.trim()
        val foto = estado.foto

        val error = controlador.validar(foto != null, nombre, telefono)
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
        if (!ubicacionLista()) return
        if (!RedLocal.concedido(this)) {
            estado.alerta = alertaRedLocalDenegada(this)
            return
        }

        enviar(nombre, telefono, estado.latitud!!, estado.longitud!!, foto!!)
    }

    /** Sube el contacto a la API. Se puede llamar de nuevo desde "Reintentar". */
    private fun enviar(nombre: String, telefono: String, latitud: Double, longitud: Double, foto: File) {
        estado.cargando = true

        trabajo.execute {
            val aSubir = try {
                ImagenJpeg.ajustarAlLimite(foto, ApiConfig.TAMANO_MAXIMO_BYTES)
            } catch (ex: IOException) {
                runOnUiThread {
                    estado.cargando = false
                    estado.alerta = AlertaUi(
                        TipoAlerta.ERROR, "Fotografía no válida", ex.message ?: "No se pudo leer la fotografía.",
                    )
                }
                return@execute
            }

            llamadaActual = ContactosApi.crear(
                nombre, telefono, latitud, longitud, aSubir,
                object : ContactosApi.Respuesta<Contacto> {
                    override fun alExito(resultado: Contacto) {
                        runOnUiThread { alGuardar(resultado) }
                    }

                    override fun alError(error: ErrorApi) {
                        runOnUiThread {
                            alErrorDeApi(error) { enviar(nombre, telefono, latitud, longitud, foto) }
                        }
                    }
                },
            )
        }
    }

    private fun alGuardar(contacto: Contacto) {
        if (isFinishing || isDestroyed) return
        estado.cargando = false
        estado.aviso = AvisoUi(TipoAlerta.EXITO, "${contacto.nombre} se guardó correctamente")
        limpiarFormulario()
    }

    private fun alErrorDeApi(error: ErrorApi, reintentar: () -> Unit) {
        if (isFinishing || isDestroyed) return
        estado.cargando = false

        estado.alerta = if (error.esSinConexion()) {
            AlertaUi(
                tipo = TipoAlerta.ERROR,
                titulo = "Sin conexión",
                mensaje = error.mensaje,
                textoAceptar = getString(R.string.reintentar),
                textoCancelar = getString(R.string.cancelar),
                alAceptar = reintentar,
            )
        } else {
            AlertaUi(TipoAlerta.ERROR, "No se pudo guardar", error.mensaje)
        }
    }

    /** Deja listo el formulario para otro contacto (la ubicacion se conserva: sigue siendo la actual). */
    private fun limpiarFormulario() {
        estado.nombre = ""
        estado.telefono = ""
        estado.errorNombre = null
        estado.errorTelefono = null
        estado.foto = null
    }
}
