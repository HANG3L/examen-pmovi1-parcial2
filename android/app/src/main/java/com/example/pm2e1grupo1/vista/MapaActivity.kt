package com.example.pm2e1grupo1.vista

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pm2e1grupo1.ui.PantallaMapa
import com.example.pm2e1grupo1.ui.TemaApp

/** Pantalla 3 (Jetpack Compose): punto del contacto en el mapa de Google Maps. */
class MapaActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val nombre = intent.getStringExtra(EXTRA_NOMBRE).orEmpty()
        val latitud = intent.getDoubleExtra(EXTRA_LATITUD, 0.0)
        val longitud = intent.getDoubleExtra(EXTRA_LONGITUD, 0.0)

        setContent {
            TemaApp {
                PantallaMapa(
                    nombre = nombre,
                    latitud = latitud,
                    longitud = longitud,
                    faltaApiKey = apiKeyVacia(),
                    alAtras = ::finish,
                )
            }
        }
    }

    /** La key llega al manifest desde local.properties; si no se configuro, el mapa sale en blanco. */
    private fun apiKeyVacia(): Boolean {
        val datos = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).metaData
        return datos?.getString("com.google.android.geo.API_KEY").isNullOrBlank()
    }

    companion object {
        const val EXTRA_NOMBRE = "nombre"
        const val EXTRA_LATITUD = "latitud"
        const val EXTRA_LONGITUD = "longitud"
    }
}
