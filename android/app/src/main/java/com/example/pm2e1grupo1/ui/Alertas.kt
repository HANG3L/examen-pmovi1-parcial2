package com.example.pm2e1grupo1.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pm2e1grupo1.R

/** Tipos de alerta: cada uno con su color, su fondo y su icono. */
enum class TipoAlerta(
    @param:ColorRes val color: Int,
    @param:ColorRes val fondo: Int,
    @param:DrawableRes val icono: Int,
) {
    EXITO(R.color.alerta_exito, R.color.alerta_exito_fondo, R.drawable.ic_exito),
    ERROR(R.color.alerta_error, R.color.alerta_error_fondo, R.drawable.ic_error),
    ADVERTENCIA(R.color.alerta_aviso, R.color.alerta_aviso_fondo, R.drawable.ic_advertencia),
    INFO(R.color.alerta_info, R.color.alerta_info_fondo, R.drawable.ic_info),
}

/**
 * Datos de una alerta (dialogo). Con textoCancelar es una confirmacion:
 * alAceptar solo se ejecuta si el usuario pulsa el boton de aceptar.
 */
data class AlertaUi(
    val tipo: TipoAlerta,
    val titulo: String,
    val mensaje: String,
    val textoAceptar: String? = null,
    val textoCancelar: String? = null,
    @param:DrawableRes val icono: Int? = null,
    val alAceptar: (() -> Unit)? = null,
)

/** Mensaje corto en la parte inferior, sin interrumpir. */
data class AvisoUi(val tipo: TipoAlerta, val mensaje: String)

/** Dialogo con el diseno comun de todas las alertas de la app. */
@Composable
fun DialogoAlerta(alerta: AlertaUi, alCerrar: () -> Unit) {
    val color = colorResource(alerta.tipo.color)
    val fondo = colorResource(alerta.tipo.fondo)

    AlertDialog(
        onDismissRequest = alCerrar,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(fondo, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(alerta.icono ?: alerta.tipo.icono),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp),
                )
            }
        },
        title = {
            Text(alerta.titulo, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            Text(alerta.mensaje, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Button(
                onClick = {
                    alCerrar()
                    alerta.alAceptar?.invoke()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = color,
                    contentColor = colorResource(R.color.sobre_alerta),
                ),
            ) {
                Text(alerta.textoAceptar ?: stringResource(R.string.aceptar))
            }
        },
        dismissButton = alerta.textoCancelar?.let { texto ->
            { TextButton(onClick = alCerrar) { Text(texto) } }
        },
    )
}

/** Fotografia de un contacto en grande, descargada de la API. */
@Composable
fun DialogoFoto(titulo: String, archivoUrl: String, alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = {
            FotoApi(
                archivoUrl = archivoUrl,
                ladoMaximo = 1024,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                escala = ContentScale.Fit,
            )
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text(stringResource(R.string.cerrar)) } },
    )
}

/** Snackbar con el color y el icono del tipo de aviso. */
@Composable
fun HostAvisos(estado: SnackbarHostState) {
    SnackbarHost(estado) { datos ->
        val tipo = (datos.visuals as? AvisoVisuals)?.tipo ?: TipoAlerta.INFO
        Snackbar(
            containerColor = colorResource(tipo.fondo),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(tipo.icono),
                    contentDescription = null,
                    tint = colorResource(tipo.color),
                    modifier = Modifier.size(24.dp),
                )
                Box(Modifier.width(12.dp))
                Text(datos.visuals.message)
            }
        }
    }
}

class AvisoVisuals(val tipo: TipoAlerta, override val message: String) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Long
}

/** Muestra el aviso pendiente (si hay) y lo marca como mostrado. */
@Composable
fun EfectoAviso(estado: EstadoBase, host: SnackbarHostState) {
    val aviso = estado.aviso
    LaunchedEffect(aviso) {
        if (aviso != null) {
            estado.aviso = null
            host.showSnackbar(AvisoVisuals(aviso.tipo, aviso.mensaje))
        }
    }
}

/** Alerta para un permiso denegado; si fue con "no volver a preguntar", ofrece abrir los ajustes. */
fun alertaPermisoDenegado(actividad: Activity, permiso: String, titulo: String, mensaje: String): AlertaUi {
    if (actividad.shouldShowRequestPermissionRationale(permiso)) {
        return AlertaUi(TipoAlerta.ADVERTENCIA, titulo, mensaje)
    }
    return AlertaUi(
        tipo = TipoAlerta.ADVERTENCIA,
        titulo = titulo,
        mensaje = "$mensaje Puedes concederlo desde los ajustes de la aplicación.",
        textoAceptar = actividad.getString(R.string.abrir_ajustes),
        textoCancelar = actividad.getString(R.string.cancelar),
        alAceptar = {
            actividad.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", actividad.packageName, null),
                ),
            )
        },
    )
}
