package com.example.pm2e1grupo1.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import com.example.pm2e1grupo1.R;

/** Mensajes comunes cuando el usuario niega un permiso. */
public final class Permisos {

    private Permisos() {
    }

    /**
     * Si el permiso se nego con "no volver a preguntar", pedirlo otra vez no hace nada:
     * se ofrece abrir los ajustes de la app para concederlo a mano.
     */
    public static void avisarDenegado(Activity actividad, String permiso, String titulo, String mensaje) {
        if (actividad.shouldShowRequestPermissionRationale(permiso)) {
            Alertas.mostrar(actividad, Alertas.Tipo.ADVERTENCIA, titulo, mensaje);
        } else {
            Alertas.confirmar(actividad, Alertas.Tipo.ADVERTENCIA, R.drawable.ic_advertencia, titulo,
                    mensaje + " Puedes concederlo desde los ajustes de la aplicación.",
                    actividad.getString(R.string.abrir_ajustes), actividad.getString(R.string.cancelar),
                    () -> actividad.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", actividad.getPackageName(), null))));
        }
    }
}
