package com.example.pm2e1grupo1.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Permiso de red local (Android 17 / API 37 en adelante).
 *
 * Desde Android 17, una app que apunta a SDK 37 no puede conectarse a direcciones de la red
 * local (192.168.x.x, 10.x.x.x... incluida 10.0.2.2 del emulador) sin el permiso
 * ACCESS_LOCAL_NETWORK. Sin el, Android descarta las conexiones en silencio y OkHttp
 * termina en "timeout". La API del examen corre en la PC, que esta en la red local.
 */
object RedLocal {

    const val PERMISO = "android.permission.ACCESS_LOCAL_NETWORK"

    /** En versiones anteriores a Android 17 el permiso no existe y no hace falta pedirlo. */
    val requerido: Boolean
        get() = Build.VERSION.SDK_INT >= 37

    fun concedido(contexto: Context): Boolean =
        !requerido ||
            ContextCompat.checkSelfPermission(contexto, PERMISO) == PackageManager.PERMISSION_GRANTED
}
