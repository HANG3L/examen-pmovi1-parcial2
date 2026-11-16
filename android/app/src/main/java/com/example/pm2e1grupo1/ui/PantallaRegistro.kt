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

/** Estado de la pantalla 1: lo que el usuario escribio, la foto tomada y la ubicacion del GPS. */
class RegistroEstado : EstadoBase() {
    var nombre by mutableStateOf("")
    var telefono by mutableStateOf("")
    var errorNombre by mutableStateOf<String?>(null)
    var errorTelefono by mutableStateOf<String?>(null)
    var foto by mutableStateOf<File?>(null)
    var latitud by mutableStateOf<Double?>(null)
    var longitud by mutableStateOf<Double?>(null)
}

/** Pantalla 1: registro de un contacto con fotografia y ubicacion. */
@Composable
fun PantallaRegistro(
    estado: RegistroEstado,
    alTomarFoto: () -> Unit,
    alGuardar: () -> Unit,
    alVerContactos: () -> Unit,
) {
    MarcoPantalla(estado, titulo = stringResource(R.string.titulo_registro)) { margenes ->
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
                val foto = estado.foto
                if (foto != null) {
                    FotoArchivo(foto, ladoMaximo = 1024, modifier = modificador)
                } else {
                    FotoVacia(modificador)
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
                latitud = estado.latitud?.let { String.format(Locale.US, "%.6f", it) } ?: "",
                longitud = estado.longitud?.let { String.format(Locale.US, "%.6f", it) } ?: "",
                ayudaUbicacion = stringResource(R.string.ayuda_ubicacion),
                modifier = Modifier.padding(top = 12.dp),
            )

            BotonGrande(
                texto = stringResource(R.string.salvar_contacto),
                icono = R.drawable.ic_guardar,
                alPulsar = alGuardar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            )
            BotonGrande(
                texto = stringResource(R.string.contactos_salvados),
                icono = R.drawable.ic_grupo,
                alPulsar = alVerContactos,
                principal = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 20.dp),
            )
        }
    }
}
