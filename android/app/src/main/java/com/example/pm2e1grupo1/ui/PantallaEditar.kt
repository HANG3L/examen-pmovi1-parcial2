package com.example.pm2e1grupo1.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pm2e1grupo1.R
import java.io.File
import java.util.Locale

/** Estado de la pantalla de edicion: datos actuales del contacto y, opcionalmente, una foto nueva. */
class EditarEstado(
    nombre: String,
    telefono: String,
    val latitud: Double,
    val longitud: Double,
    val archivoUrl: String,
) : EstadoBase() {
    var nombre by mutableStateOf(nombre)
    var telefono by mutableStateOf(telefono)
    var errorNombre by mutableStateOf<String?>(null)
    var errorTelefono by mutableStateOf<String?>(null)

    /** Fotografia nueva (null: se conserva la que ya tiene el contacto en la API). */
    var fotoNueva by mutableStateOf<File?>(null)
}

/** Actualizar un contacto (PUT). La fotografia es opcional. */
@Composable
fun PantallaEditar(
    estado: EditarEstado,
    alAtras: () -> Unit,
    alTomarFoto: () -> Unit,
    alGuardar: () -> Unit,
) {
    MarcoPantalla(estado, titulo = stringResource(R.string.titulo_editar), alAtras = alAtras) { margenes ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(margenes)
                .consumeWindowInsets(margenes)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            TarjetaFoto(alTomar = alTomarFoto) { modificador ->
                val nueva = estado.fotoNueva
                if (nueva != null) {
                    FotoArchivo(nueva, ladoMaximo = 1024, modifier = modificador)
                } else {
                    FotoApi(estado.archivoUrl, ladoMaximo = 512, modifier = modificador)
                }
            }

            TarjetaDatos(
                nombre = estado.nombre,
                alCambiarNombre = {
                    estado.nombre = it
                    estado.errorNombre = null
                },
                errorNombre = estado.errorNombre,
                telefono = estado.telefono,
                alCambiarTelefono = {
                    estado.telefono = it
                    estado.errorTelefono = null
                },
                errorTelefono = estado.errorTelefono,
                latitud = String.format(Locale.US, "%.6f", estado.latitud),
                longitud = String.format(Locale.US, "%.6f", estado.longitud),
                ayudaUbicacion = null,
                modifier = Modifier.padding(top = 12.dp),
            )

            BotonGrande(
                texto = stringResource(R.string.guardar_cambios),
                icono = R.drawable.ic_guardar,
                alPulsar = alGuardar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 20.dp),
            )
        }
    }
}
