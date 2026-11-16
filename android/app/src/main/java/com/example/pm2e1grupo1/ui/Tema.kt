package com.example.pm2e1grupo1.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import com.example.pm2e1grupo1.R

/**
 * Tema Material 3 de la app. Los colores salen de res/values (modo claro) y
 * res/values-night (modo oscuro), asi que la paleta es la misma de siempre.
 */
@Composable
fun TemaApp(contenido: @Composable () -> Unit) {
    val base = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    val colores = base.copy(
        primary = colorResource(R.color.primario),
        onPrimary = colorResource(R.color.sobre_primario),
        primaryContainer = colorResource(R.color.primario_contenedor),
        onPrimaryContainer = colorResource(R.color.sobre_primario_contenedor),
        secondary = colorResource(R.color.secundario),
        onSecondary = colorResource(R.color.sobre_secundario),
        secondaryContainer = colorResource(R.color.secundario_contenedor),
        onSecondaryContainer = colorResource(R.color.sobre_secundario_contenedor),
        error = colorResource(R.color.error),
        onError = colorResource(R.color.sobre_error),
        errorContainer = colorResource(R.color.error_contenedor),
        onErrorContainer = colorResource(R.color.sobre_error_contenedor),
        background = colorResource(R.color.fondo),
        onBackground = colorResource(R.color.texto),
        surface = colorResource(R.color.tarjeta),
        onSurface = colorResource(R.color.texto),
        surfaceVariant = colorResource(R.color.superficie_variante),
        onSurfaceVariant = colorResource(R.color.texto_secundario),
        outline = colorResource(R.color.borde),
    )

    MaterialTheme(colorScheme = colores, content = contenido)
}
