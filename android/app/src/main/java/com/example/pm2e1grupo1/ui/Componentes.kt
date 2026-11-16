package com.example.pm2e1grupo1.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pm2e1grupo1.R
import com.example.pm2e1grupo1.red.CargadorImagenes
import com.example.pm2e1grupo1.util.ImagenJpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Estado comun de toda pantalla: alerta abierta, aviso pendiente e indicador de carga. */
open class EstadoBase {
    var alerta by mutableStateOf<AlertaUi?>(null)
    var aviso by mutableStateOf<AvisoUi?>(null)
    var cargando by mutableStateOf(false)
}

/**
 * Marco de todas las pantallas: barra superior (con flecha atras si se indica), avisos,
 * alertas y, opcionalmente, una capa que bloquea la pantalla mientras se espera a la API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarcoPantalla(
    estado: EstadoBase,
    titulo: String,
    alAtras: (() -> Unit)? = null,
    bloquearAlCargar: Boolean = true,
    contenido: @Composable (PaddingValues) -> Unit,
) {
    val host = remember { SnackbarHostState() }
    EfectoAviso(estado, host)

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(titulo, style = MaterialTheme.typography.headlineSmall) },
                    navigationIcon = {
                        if (alAtras != null) {
                            IconButton(onClick = alAtras) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_atras),
                                    contentDescription = stringResource(R.string.volver),
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
            snackbarHost = { HostAvisos(host) },
            content = contenido,
        )

        if (estado.cargando && bloquearAlCargar) {
            // Tapa la pantalla y se traga los toques mientras se espera la respuesta.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .pointerInput(Unit) { detectTapGestures { } },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }

    estado.alerta?.let { alerta -> DialogoAlerta(alerta) { estado.alerta = null } }
}

// ---------- Fotografias ----------

/** Silueta que se ve mientras llega la fotografia o si no se pudo descargar. */
@Composable
fun FotoVacia(modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_persona),
            contentDescription = stringResource(R.string.foto_contacto),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(40.dp),
        )
    }
}

/** Fotografia del contacto descargada de la API con OkHttp. */
@Composable
fun FotoApi(
    archivoUrl: String,
    ladoMaximo: Int,
    modifier: Modifier = Modifier,
    escala: ContentScale = ContentScale.Crop,
) {
    var imagen by remember(archivoUrl, ladoMaximo) {
        mutableStateOf<Bitmap?>(CargadorImagenes.enCache(archivoUrl, ladoMaximo))
    }

    DisposableEffect(archivoUrl, ladoMaximo) {
        var activo = true
        if (imagen == null) {
            CargadorImagenes.cargar(archivoUrl, ladoMaximo) { descargada ->
                if (activo) imagen = descargada
            }
        }
        onDispose { activo = false }
    }

    val actual = imagen
    if (actual != null) {
        Image(
            bitmap = actual.asImageBitmap(),
            contentDescription = stringResource(R.string.foto_contacto),
            contentScale = escala,
            modifier = modifier,
        )
    } else {
        FotoVacia(modifier)
    }
}

/** Fotografia recien tomada con la camara (archivo local). */
@Composable
fun FotoArchivo(archivo: File, ladoMaximo: Int, modifier: Modifier = Modifier) {
    val imagen by produceState<Bitmap?>(null, archivo) {
        value = withContext(Dispatchers.IO) { ImagenJpeg.cargarMiniatura(archivo, ladoMaximo) }
    }

    val actual = imagen
    if (actual != null) {
        Image(
            bitmap = actual.asImageBitmap(),
            contentDescription = stringResource(R.string.foto_contacto),
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        FotoVacia(modifier)
    }
}

// ---------- Formulario de contacto ----------

@Composable
private fun TarjetaContenedor(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp)) { contenido() }
    }
}

/** Tarjeta con la fotografia (la que da `foto`) y el boton para tomarla. */
@Composable
fun TarjetaFoto(alTomar: () -> Unit, foto: @Composable (Modifier) -> Unit) {
    TarjetaContenedor {
        foto(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        FilledTonalButton(
            onClick = alTomar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            Icon(painterResource(R.drawable.ic_camara), contentDescription = null, Modifier.size(18.dp))
            Text(stringResource(R.string.tomar_fotografia), Modifier.padding(start = 8.dp))
        }
    }
}

/** Tarjeta con nombre, telefono y ubicacion (latitud y longitud bloqueadas). */
@Composable
fun TarjetaDatos(
    nombre: String,
    alCambiarNombre: (String) -> Unit,
    errorNombre: String?,
    telefono: String,
    alCambiarTelefono: (String) -> Unit,
    errorTelefono: String?,
    latitud: String,
    longitud: String,
    ayudaUbicacion: String?,
    modifier: Modifier = Modifier,
) {
    TarjetaContenedor(modifier) {
        Text(
            text = stringResource(R.string.datos_contacto),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = nombre,
                onValueChange = alCambiarNombre,
                label = { Text(stringResource(R.string.nombre)) },
                leadingIcon = { IconoCampo(R.drawable.ic_persona) },
                isError = errorNombre != null,
                supportingText = { Text(errorNombre ?: stringResource(R.string.ayuda_nombre)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = telefono,
                onValueChange = { nuevo -> alCambiarTelefono(nuevo.filter { it.isDigit() }.take(8)) },
                label = { Text(stringResource(R.string.telefono)) },
                leadingIcon = { IconoCampo(R.drawable.ic_llamar) },
                isError = errorTelefono != null,
                supportingText = {
                    Text(errorTelefono ?: "${stringResource(R.string.ayuda_telefono)} (${telefono.length}/8)")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )

            // Bloqueados: los llena el GPS (o vienen de la API al editar), el usuario no los escribe.
            OutlinedTextField(
                value = latitud,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.latitud)) },
                leadingIcon = { IconoCampo(R.drawable.ic_mundo) },
                supportingText = ayudaUbicacion?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = longitud,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.longitud)) },
                leadingIcon = { IconoCampo(R.drawable.ic_mundo) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun IconoCampo(icono: Int) {
    Icon(painterResource(icono), contentDescription = null, Modifier.size(24.dp))
}

/** Boton grande de las pantallas: con icono y texto. */
@Composable
fun BotonGrande(
    texto: String,
    icono: Int,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier,
    principal: Boolean = true,
    enabled: Boolean = true,
) {
    val contenido: @Composable () -> Unit = {
        Icon(painterResource(icono), contentDescription = null, Modifier.size(20.dp))
        Text(texto, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
    }
    if (principal) {
        Button(
            onClick = alPulsar,
            enabled = enabled,
            modifier = modifier.height(56.dp),
            content = { contenido() },
        )
    } else {
        androidx.compose.material3.OutlinedButton(
            onClick = alPulsar,
            enabled = enabled,
            modifier = modifier.height(56.dp),
            content = { contenido() },
        )
    }
}
