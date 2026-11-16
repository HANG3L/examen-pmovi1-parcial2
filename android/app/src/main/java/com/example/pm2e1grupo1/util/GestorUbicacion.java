package com.example.pm2e1grupo1.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Ubicacion en tiempo real con FusedLocationProviderClient.
 * Se inicia en onResume y se detiene en onPause para no gastar bateria con la pantalla cerrada.
 */
public class GestorUbicacion {

    public interface Oyente {
        void alCambiarUbicacion(double latitud, double longitud);
    }

    private static final long INTERVALO_MS = 5000;

    private final Context contexto;
    private final FusedLocationProviderClient cliente;
    private final Oyente oyente;
    private boolean activo;

    private final LocationCallback retorno = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult resultado) {
            Location ubicacion = resultado.getLastLocation();
            if (ubicacion != null) {
                oyente.alCambiarUbicacion(ubicacion.getLatitude(), ubicacion.getLongitude());
            }
        }
    };

    public GestorUbicacion(Context contexto, Oyente oyente) {
        this.contexto = contexto;
        this.oyente = oyente;
        this.cliente = LocationServices.getFusedLocationProviderClient(contexto);
    }

    public boolean tienePermiso() {
        return ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** true si el GPS / servicio de ubicacion del telefono esta encendido. */
    public boolean gpsActivo() {
        LocationManager administrador = ContextCompat.getSystemService(contexto, LocationManager.class);
        return administrador != null && LocationManagerCompat.isLocationEnabled(administrador);
    }

    /** Empieza a recibir ubicaciones. No hace nada si falta el permiso o el GPS esta apagado. */
    @SuppressLint("MissingPermission") // se comprueba con tienePermiso()
    public void iniciar() {
        if (activo || !tienePermiso() || !gpsActivo()) {
            return;
        }
        LocationRequest peticion = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVALO_MS)
                .setMinUpdateIntervalMillis(INTERVALO_MS / 2)
                .build();
        cliente.requestLocationUpdates(peticion, retorno, Looper.getMainLooper());
        activo = true;

        // Una primera posicion rapida mientras llega la lectura del GPS.
        cliente.getLastLocation().addOnSuccessListener(ubicacion -> {
            if (ubicacion != null) {
                oyente.alCambiarUbicacion(ubicacion.getLatitude(), ubicacion.getLongitude());
            }
        });
    }

    public void detener() {
        if (activo) {
            cliente.removeLocationUpdates(retorno);
            activo = false;
        }
    }
}
