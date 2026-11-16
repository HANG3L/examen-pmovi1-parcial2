package com.example.pm2e1grupo1.vista

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.pm2e1grupo1.R
import com.example.pm2e1grupo1.modelo.Contacto
import com.example.pm2e1grupo1.red.ContactosApi
import com.example.pm2e1grupo1.red.ErrorApi
import com.example.pm2e1grupo1.ui.AlertaUi
import com.example.pm2e1grupo1.ui.AvisoUi
import com.example.pm2e1grupo1.ui.ListaEstado
import com.example.pm2e1grupo1.ui.PantallaLista
import com.example.pm2e1grupo1.ui.TemaApp
import com.example.pm2e1grupo1.ui.TipoAlerta
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
                    alEliminar = ::pedirConfirmacionEliminar,
                    alActualizar = ::abrirEdicion,
                )
            }
        }
    }

    override fun onDestroy() {
        llamadaActual?.cancel()
        super.onDestroy()
    }

    // ---------- Lista (GET) y busqueda ----------

    private fun cargar() {
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

    // ---------- Seleccion y ubicacion ----------

    private fun alTocarContacto(contacto: Contacto) {
        estado.idSeleccionado = contacto.id
        estado.alerta = AlertaUi(
            tipo = TipoAlerta.INFO,
            titulo = "Acción",
            mensaje = "Desea ir a la ubicación de ${contacto.nombre}?",
            textoAceptar = getString(R.string.si),
            textoCancelar = getString(R.string.no),
            icono = R.drawable.ic_mundo,
            alAceptar = { irALaUbicacion(contacto) },
        )
    }

    /** El mapa (pantalla 3) se agrega en el ultimo paso del examen. */
    private fun irALaUbicacion(contacto: Contacto) {
        estado.alerta = AlertaUi(
            TipoAlerta.INFO, "Próximamente",
            "El mapa con la ubicación de ${contacto.nombre} se agrega en el último paso.",
        )
    }

    // ---------- Actualizar (PUT) ----------

    private fun abrirEdicion() {
        val contacto = estado.seleccionado()
        if (contacto == null) {
            avisarSinSeleccion()
            return
        }
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

    private fun pedirConfirmacionEliminar() {
        val contacto = estado.seleccionado()
        if (contacto == null) {
            avisarSinSeleccion()
            return
        }
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

    private fun avisarSinSeleccion() {
        estado.alerta = AlertaUi(
            TipoAlerta.ADVERTENCIA, "Seleccione un contacto",
            "Toque un contacto de la lista para elegirlo.",
        )
    }

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
