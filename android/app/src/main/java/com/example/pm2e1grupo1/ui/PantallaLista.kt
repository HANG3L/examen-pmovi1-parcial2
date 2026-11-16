package com.example.pm2e1grupo1.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pm2e1grupo1.R
import com.example.pm2e1grupo1.modelo.Contacto
import kotlinx.coroutines.delay
import java.util.Locale

/** Estado de la pantalla 2: contactos cargados, texto de busqueda y contacto seleccionado. */
class ListaEstado : EstadoBase() {
    val contactos = mutableStateListOf<Contacto>()
    var buscar by mutableStateOf("")
    var idSeleccionado by mutableStateOf(-1)

    /** Contacto que se esta viendo en grande (null = ninguno). */
    var fotoGrande by mutableStateOf<Contacto?>(null)

    /** Texto cuando no hay contactos que mostrar (null mientras hay lista o se esta cargando). */
    var mensajeVacio by mutableStateOf<Int?>(null)

    fun seleccionado(): Contacto? = contactos.firstOrNull { it.id == idSeleccionado }
}

private const val ESPERA_BUSQUEDA_MS = 350L

/** Pantalla 2: lista de contactos de la API con busqueda, actualizar y eliminar. */
@Composable
fun PantallaLista(
    estado: ListaEstado,
    alAtras: () -> Unit,
    alBuscar: () -> Unit,
    alTocar: (Contacto) -> Unit,
    alEliminar: () -> Unit,
    alActualizar: () -> Unit,
) {
    // Primera carga inmediata; despues de cada letra se espera a que el usuario deje de escribir.
    var cargoPrimeraVez by remember { mutableStateOf(false) }
    LaunchedEffect(estado.buscar) {
        if (cargoPrimeraVez) delay(ESPERA_BUSQUEDA_MS)
        cargoPrimeraVez = true
        alBuscar()
    }

    MarcoPantalla(
        estado = estado,
        titulo = stringResource(R.string.contactos_salvados),
        alAtras = alAtras,
        bloquearAlCargar = false,
    ) { margenes ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(margenes),
        ) {
            OutlinedTextField(
                value = estado.buscar,
                onValueChange = { estado.buscar = it },
                label = { Text(stringResource(R.string.buscar)) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_buscar), contentDescription = null, Modifier.size(24.dp))
                },
                trailingIcon = {
                    if (estado.buscar.isNotEmpty()) {
                        IconButton(onClick = { estado.buscar = "" }) {
                            Icon(
                                painterResource(R.drawable.ic_error),
                                contentDescription = stringResource(R.string.limpiar),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp, vertical = 8.dp,
                    ),
                ) {
                    items(estado.contactos, key = { it.id }) { contacto ->
                        FilaContacto(
                            contacto = contacto,
                            seleccionado = contacto.id == estado.idSeleccionado,
                            alTocar = { alTocar(contacto) },
                            alTocarFoto = { estado.fotoGrande = contacto },
                        )
                    }
                }

                estado.mensajeVacio?.let { mensaje ->
                    Text(
                        text = stringResource(mensaje),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )
                }

                if (estado.cargando) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BotonGrande(
                    texto = stringResource(R.string.eliminar_contacto),
                    icono = R.drawable.ic_eliminar,
                    alPulsar = alEliminar,
                    principal = false,
                    enabled = !estado.cargando,
                    modifier = Modifier.weight(1f),
                )
                BotonGrande(
                    texto = stringResource(R.string.actualizar_contacto),
                    icono = R.drawable.ic_editar,
                    alPulsar = alActualizar,
                    enabled = !estado.cargando,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    estado.fotoGrande?.let { contacto ->
        DialogoFoto(contacto.nombre, contacto.archivoUrl) { estado.fotoGrande = null }
    }
}

@Composable
private fun FilaContacto(
    contacto: Contacto,
    seleccionado: Boolean,
    alTocar: () -> Unit,
    alTocarFoto: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = alTocar),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Fotografia descargada de la API; al tocarla se ve en grande.
            FotoApi(
                archivoUrl = contacto.archivoUrl,
                ladoMaximo = 160,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable(onClick = alTocarFoto),
            )

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    text = contacto.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatearTelefono(contacto.telefono),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = String.format(Locale.US, "Lat %.4f · Lng %.4f", contacto.latitud, contacto.longitud),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 99765645 -> 9976-5645 */
private fun formatearTelefono(telefono: String?): String =
    if (telefono != null && telefono.length == 8) "${telefono.substring(0, 4)}-${telefono.substring(4)}" else telefono ?: ""
