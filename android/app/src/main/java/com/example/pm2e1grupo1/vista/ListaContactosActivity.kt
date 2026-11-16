package com.example.pm2e1grupo1.vista

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.pm2e1grupo1.R
import com.example.pm2e1grupo1.modelo.Contacto
import com.example.pm2e1grupo1.red.ContactosApi
import com.example.pm2e1grupo1.red.ErrorApi
import com.example.pm2e1grupo1.ui.AccionContacto
import com.example.pm2e1grupo1.ui.AlertaUi
import com.example.pm2e1grupo1.ui.alertaPermisoDenegado
import com.example.pm2e1grupo1.ui.AvisoUi
import com.example.pm2e1grupo1.ui.ListaEstado
import com.example.pm2e1grupo1.ui.PantallaLista
import com.example.pm2e1grupo1.ui.TemaApp
import com.example.pm2e1grupo1.ui.TipoAlerta
import com.example.pm2e1grupo1.ui.alertaRedLocalDenegada
import com.example.pm2e1grupo1.util.RedLocal
import okhttp3.Call

/** Pantalla 2 (Jetpack Compose): lista de contactos de la API con busqueda, actualizar y eliminar. */
class ListaContactosActivity : ComponentActivity() {

    private val estado = ListaEstado()
    private var llamadaActual: Call? = null

    private val lanzadorEdicion =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            if (resultado.resultCode == RESULT_OK) {
                estado.aviso = AvisoUi(TipoAlerta.EXITO, "Contacto actualizado")
                cargar()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TemaApp {
                PantallaLista(
                    estado = estado,
                    alAtras = ::finish,
                    alBuscar = ::cargar,
                    alTocar = ::alTocarContacto,
                    alElegirAccion = ::alElegirAccion,
                )
            }
        }
    }

    override fun onDestroy() {
        llamadaActual?.cancel()
        super.onDestroy()
    }

    // ---------- Lista (GET) y busqueda ----------

    /** Android 17+: sin permiso de red local no se llega a la API (se pide una sola vez). */
    private var permisoRedLocalSolicitado = false

    private val lanzadorRedLocal =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            if (concedido) cargar() else estado.alerta = alertaRedLocalDenegada(this)
        }

    private fun cargar() {
        if (!RedLocal.concedido(this)) {
            if (!permisoRedLocalSolicitado) {
                permisoRedLocalSolicitado = true
                lanzadorRedLocal.launch(RedLocal.PERMISO)
            } else {
                estado.alerta = alertaRedLocalDenegada(this)
            }
            return
        }

        llamadaActual?.cancel()
        val buscar = estado.buscar.trim()
        estado.cargando = true

        llamadaActual = ContactosApi.listar(buscar, object : ContactosApi.Respuesta<List<Contacto>> {
            override fun alExito(resultado: List<Contacto>) {
                runOnUiThread { alListar(resultado, buscar) }
            }

            override fun alError(error: ErrorApi) {
                runOnUiThread { alErrorDeApi(error, "No se pudo cargar la lista", ::cargar) }
            }
        })
    }

    private fun alListar(contactos: List<Contacto>, buscar: String) {
        if (isFinishing || isDestroyed) return
        estado.cargando = false
        estado.contactos.clear()
        estado.contactos.addAll(contactos)

        if (estado.seleccionado() == null) estado.idSeleccionado = -1 // ya no esta en la lista

        estado.mensajeVacio = when {
            contactos.isNotEmpty() -> null
            buscar.isEmpty() -> R.string.lista_vacia
            else -> R.string.sin_resultados
        }
    }

    // ---------- Seleccion y opciones del contacto ----------

    /** Al tocar un contacto queda seleccionado y se despliegan sus opciones. */
    private fun alTocarContacto(contacto: Contacto) {
        estado.idSeleccionado = contacto.id
        estado.contactoAcciones = contacto
    }

    private fun alElegirAccion(accion: AccionContacto, contacto: Contacto) {
        estado.contactoAcciones = null
        when (accion) {
            AccionContacto.EDITAR -> abrirEdicion(contacto)
            AccionContacto.ELIMINAR -> pedirConfirmacionEliminar(contacto)
            AccionContacto.LLAMAR -> pedirConfirmacionLlamada(contacto)
            AccionContacto.UBICACION -> preguntarIrALaUbicacion(contacto)
        }
    }

    // ---------- Ubicacion (pantalla 3) ----------

    /** Confirmacion del enunciado: "Desea ir a la ubicación de ...?" con No / Sí. */
    private fun preguntarIrALaUbicacion(contacto: Contacto) {
        estado.alerta = AlertaUi(
            tipo = TipoAlerta.INFO,
            titulo = getString(R.string.ver_ubicacion),
            mensaje = "¿Desea ir a la ubicación de ${contacto.nombre}?",
            textoAceptar = getString(R.string.si),
            textoCancelar = getString(R.string.no),
            icono = R.drawable.ic_mundo,
            alAceptar = { irALaUbicacion(contacto) },
        )
    }

    /** Pantalla 3: abre el mapa con el marcador del contacto. */
    private fun irALaUbicacion(contacto: Contacto) {
        val intento = Intent(this, MapaActivity::class.java)
            .putExtra(MapaActivity.EXTRA_NOMBRE, contacto.nombre)
            .putExtra(MapaActivity.EXTRA_LATITUD, contacto.latitud)
            .putExtra(MapaActivity.EXTRA_LONGITUD, contacto.longitud)
        startActivity(intento)
    }

    // ---------- Llamar ----------

    private var numeroPorLlamar: String? = null

    private val lanzadorPermisoLlamada =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            val numero = numeroPorLlamar
            numeroPorLlamar = null
            if (concedido && numero != null) {
                llamar(numero)
            } else if (!concedido) {
                estado.alerta = alertaPermisoDenegado(
                    this, Manifest.permission.CALL_PHONE, "Permiso de llamadas denegado",
                    "Sin este permiso la app no puede llamar al contacto.",
                )
            }
        }

    private fun pedirConfirmacionLlamada(contacto: Contacto) {
        estado.alerta = AlertaUi(
            tipo = TipoAlerta.EXITO,
            titulo = "Llamar",
            mensaje = "¿Desea llamar a ${contacto.nombre} (${contacto.telefono})?",
            textoAceptar = getString(R.string.llamar),
            textoCancelar = getString(R.string.cancelar),
            icono = R.drawable.ic_llamar,
            alAceptar = {
                val concedido = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) ==
                    PackageManager.PERMISSION_GRANTED
                if (concedido) {
                    llamar(contacto.telefono)
                } else {
                    numeroPorLlamar = contacto.telefono
                    lanzadorPermisoLlamada.launch(Manifest.permission.CALL_PHONE)
                }
            },
        )
    }

    /** ACTION_CALL marca directamente (requiere CALL_PHONE concedido). */
    private fun llamar(numero: String) {
        try {
            startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$numero")))
        } catch (ex: ActivityNotFoundException) {
            estado.alerta = AlertaUi(
                TipoAlerta.INFO, "Llamadas no disponibles",
                "Este dispositivo no puede realizar llamadas.",
            )
        }
    }

    // ---------- Actualizar (PUT) ----------

    private fun abrirEdicion(contacto: Contacto) {
        val intento = Intent(this, EditarContactoActivity::class.java)
            .putExtra(EditarContactoActivity.EXTRA_ID, contacto.id)
            .putExtra(EditarContactoActivity.EXTRA_NOMBRE, contacto.nombre)
            .putExtra(EditarContactoActivity.EXTRA_TELEFONO, contacto.telefono)
            .putExtra(EditarContactoActivity.EXTRA_LATITUD, contacto.latitud)
            .putExtra(EditarContactoActivity.EXTRA_LONGITUD, contacto.longitud)
            .putExtra(EditarContactoActivity.EXTRA_ARCHIVO_URL, contacto.archivoUrl)
        lanzadorEdicion.launch(intento)
    }

    // ---------- Eliminar (DELETE) ----------

    private fun pedirConfirmacionEliminar(contacto: Contacto) {
        estado.alerta = AlertaUi(
            tipo = TipoAlerta.ERROR,
            titulo = "Eliminar contacto",
            mensaje = "¿Desea eliminar a ${contacto.nombre}? Esta acción no se puede deshacer.",
            textoAceptar = getString(R.string.eliminar),
            textoCancelar = getString(R.string.cancelar),
            icono = R.drawable.ic_eliminar,
            alAceptar = { eliminar(contacto) },
        )
    }

    private fun eliminar(contacto: Contacto) {
        estado.cargando = true
        llamadaActual = ContactosApi.eliminar(contacto.id, object : ContactosApi.Respuesta<Void> {
            override fun alExito(resultado: Void?) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    estado.aviso = AvisoUi(TipoAlerta.EXITO, "${contacto.nombre} se eliminó")
                    cargar()
                }
            }

            override fun alError(error: ErrorApi) {
                runOnUiThread { alErrorDeApi(error, "No se pudo eliminar") { eliminar(contacto) } }
            }
        })
    }

    // ---------- Comunes ----------

    /** Error de conectividad: mensaje con Reintentar. Error del servidor: alerta con su mensaje. */
    private fun alErrorDeApi(error: ErrorApi, titulo: String, reintentar: () -> Unit) {
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
            AlertaUi(TipoAlerta.ERROR, titulo, error.mensaje)
        }
    }
}
