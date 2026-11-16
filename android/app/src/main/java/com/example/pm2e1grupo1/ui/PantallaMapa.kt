package com.example.pm2e1grupo1.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pm2e1grupo1.R
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

/**
 * Pantalla 3: mapa con un marcador en la ubicacion del contacto.
 * Equivale al onMapReady() del enunciado: addMarker(position, title) + moveCamera(latLng).
 */
@Composable
fun PantallaMapa(
    nombre: String,
    latitud: Double,
    longitud: Double,
    faltaApiKey: Boolean,
    alAtras: () -> Unit,
) {
    val ubicacion = LatLng(latitud, longitud)
    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(ubicacion, ZOOM_INICIAL)
    }

    MarcoPantalla(
        estado = EstadoBase(),
        titulo = stringResource(R.string.titulo_mapa),
        alAtras = alAtras,
    ) { margenes ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(margenes),
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camara,
            ) {
                Marker(
                    state = rememberUpdatedMarkerState(position = ubicacion),
                    title = nombre,
                )
            }

            if (faltaApiKey) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.mapa_sin_api_key),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

private const val ZOOM_INICIAL = 16f
