package com.example.pm2e1grupo1.vista;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pm2e1grupo1.R;
import com.example.pm2e1grupo1.controlador.ContactoControlador;
import com.example.pm2e1grupo1.modelo.Contacto;
import com.example.pm2e1grupo1.red.ApiConfig;
import com.example.pm2e1grupo1.red.ContactosApi;
import com.example.pm2e1grupo1.red.ErrorApi;
import com.example.pm2e1grupo1.util.Alertas;
import com.example.pm2e1grupo1.util.CapturaFoto;
import com.example.pm2e1grupo1.util.GestorUbicacion;
import com.example.pm2e1grupo1.util.ImagenJpeg;
import com.example.pm2e1grupo1.util.Permisos;
import com.example.pm2e1grupo1.util.Utilidades;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;

/** VISTA - Pantalla 1: registro de un contacto con fotografia y ubicacion, enviado a la API. */
public class MainActivity extends AppCompatActivity {

    private ImageView imgFoto;
    private Button btnTomarFoto, btnGuardar, btnVerContactos;
    private TextInputLayout campoNombre, campoTelefono;
    private EditText txtNombre, txtTelefono, txtLatitud, txtLongitud;
    private View capaCargando;

    private final ContactoControlador controlador = new ContactoControlador();
    private GestorUbicacion gestorUbicacion;

    /** Fotografia tomada con la camara (null mientras no se haya tomado). */
    private File archivoFoto;
    private CapturaFoto capturaFoto;

    private Double latitud;
    private Double longitud;

    private boolean permisoUbicacionSolicitado;
    private boolean avisoGpsMostrado;

    /** Un solo hilo de trabajo: ajustar la foto no debe correr en el hilo principal. */
    private final ExecutorService trabajo = Executors.newSingleThreadExecutor();
    private Call llamadaActual;

    // ---------- Resultados de permisos ----------

    private final ActivityResultLauncher<String[]> lanzadorPermisoUbicacion =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), resultado -> {
                if (gestorUbicacion.tienePermiso()) {
                    iniciarUbicacion();
                } else {
                    Permisos.avisarDenegado(this, Manifest.permission.ACCESS_FINE_LOCATION,
                            "Permiso de ubicación denegado",
                            "Sin acceso a la ubicación no es posible guardar el contacto.");
                }
            });

    // ---------- Ciclo de vida ----------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Utilidades.ajustarBordes(findViewById(R.id.raiz));

        imgFoto = findViewById(R.id.imgFoto);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnVerContactos = findViewById(R.id.btnVerContactos);
        campoNombre = findViewById(R.id.campoNombre);
        campoTelefono = findViewById(R.id.campoTelefono);
        txtNombre = findViewById(R.id.txtNombre);
        txtTelefono = findViewById(R.id.txtTelefono);
        txtLatitud = findViewById(R.id.txtLatitud);
        txtLongitud = findViewById(R.id.txtLongitud);
        capaCargando = findViewById(R.id.cargando);

        capturaFoto = new CapturaFoto(this, this::alTomarFoto);
        gestorUbicacion = new GestorUbicacion(this, this::alCambiarUbicacion);

        Utilidades.limpiarErrorAlEscribir(campoNombre);
        Utilidades.limpiarErrorAlEscribir(campoTelefono);

        btnTomarFoto.setOnClickListener(v -> capturaFoto.tomar());
        btnGuardar.setOnClickListener(v -> guardar());
        btnVerContactos.setOnClickListener(v ->
                startActivity(new Intent(this, ListaContactosActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        iniciarUbicacion();
    }

    @Override
    protected void onPause() {
        gestorUbicacion.detener();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (llamadaActual != null) {
            llamadaActual.cancel();
        }
        trabajo.shutdownNow();
        super.onDestroy();
    }

    // ---------- Fotografia ----------

    private void alTomarFoto(File foto) {
        archivoFoto = foto;
        Utilidades.cargarFoto(imgFoto, capturaFoto.uriDe(foto).toString());
    }

    // ---------- Ubicacion ----------

    /** Arranca la ubicacion en tiempo real, o explica por que no se puede (permiso o GPS apagado). */
    private void iniciarUbicacion() {
        if (!gestorUbicacion.tienePermiso()) {
            if (!permisoUbicacionSolicitado) {
                permisoUbicacionSolicitado = true;
                lanzadorPermisoUbicacion.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION});
            }
            return;
        }
        if (!gestorUbicacion.gpsActivo()) {
            if (!avisoGpsMostrado) {
                avisoGpsMostrado = true;
                avisarGpsApagado();
            }
            return;
        }
        avisoGpsMostrado = false;
        gestorUbicacion.iniciar();
    }

    private void alCambiarUbicacion(double nuevaLatitud, double nuevaLongitud) {
        latitud = nuevaLatitud;
        longitud = nuevaLongitud;
        txtLatitud.setText(String.format(Locale.US, "%.6f", nuevaLatitud));
        txtLongitud.setText(String.format(Locale.US, "%.6f", nuevaLongitud));
    }

    private void avisarGpsApagado() {
        Alertas.confirmar(this, Alertas.Tipo.ADVERTENCIA, R.drawable.ic_advertencia,
                "Alerta", "Gps no está activo.",
                getString(R.string.activar_gps), getString(R.string.cancelar),
                () -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
    }

    /** true si ya hay una ubicacion valida; si no, muestra la alerta que corresponda. */
    private boolean ubicacionLista() {
        if (!gestorUbicacion.tienePermiso()) {
            Permisos.avisarDenegado(this, Manifest.permission.ACCESS_FINE_LOCATION,
                    "Permiso de ubicación denegado",
                    "Sin acceso a la ubicación no es posible guardar el contacto.");
            return false;
        }
        if (!gestorUbicacion.gpsActivo()) {
            avisarGpsApagado();
            return false;
        }
        if (latitud == null || longitud == null) {
            Alertas.mostrar(this, Alertas.Tipo.ADVERTENCIA, "Ubicación requerida",
                    "Debe describir la ubicación. Espera a que el GPS la obtenga.");
            return false;
        }
        return true;
    }

    // ---------- Guardar ----------

    private void guardar() {
        String nombre = txtNombre.getText().toString().trim();
        String telefono = txtTelefono.getText().toString().trim();

        ContactoControlador.Error error = controlador.validar(archivoFoto != null, nombre, telefono);
        if (error != null) {
            mostrarError(error);
            return;
        }
        if (!ubicacionLista()) {
            return;
        }
        enviar(nombre, telefono, latitud, longitud, archivoFoto);
    }

    /** Sube el contacto a la API. Se puede llamar de nuevo desde "Reintentar". */
    private void enviar(String nombre, String telefono, double lat, double lng, File foto) {
        mostrarCarga(true);

        trabajo.execute(() -> {
            File aSubir;
            try {
                aSubir = ImagenJpeg.ajustarAlLimite(foto, ApiConfig.TAMANO_MAXIMO_BYTES);
            } catch (IOException ex) {
                runOnUiThread(() -> {
                    mostrarCarga(false);
                    Alertas.mostrar(this, Alertas.Tipo.ERROR, "Fotografía no válida", ex.getMessage());
                });
                return;
            }

            llamadaActual = ContactosApi.crear(nombre, telefono, lat, lng, aSubir,
                    new ContactosApi.Respuesta<Contacto>() {
                        @Override
                        public void alExito(Contacto contacto) {
                            runOnUiThread(() -> alGuardar(contacto));
                        }

                        @Override
                        public void alError(ErrorApi error) {
                            runOnUiThread(() -> alErrorDeApi(error,
                                    () -> enviar(nombre, telefono, lat, lng, foto)));
                        }
                    });
        });
    }

    private void alGuardar(Contacto contacto) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        mostrarCarga(false);
        Alertas.aviso(this, Alertas.Tipo.EXITO, contacto.getNombre() + " se guardó correctamente");
        limpiarFormulario();
    }

    private void alErrorDeApi(ErrorApi error, Runnable reintentar) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        mostrarCarga(false);

        if (error.esSinConexion()) {
            Alertas.confirmar(this, Alertas.Tipo.ERROR, R.drawable.ic_error, "Sin conexión",
                    error.getMensaje(), getString(R.string.reintentar), getString(R.string.cancelar),
                    reintentar);
        } else {
            Alertas.mostrar(this, Alertas.Tipo.ERROR, "No se pudo guardar", error.getMensaje());
        }
    }

    private void mostrarError(ContactoControlador.Error error) {
        switch (error.campo) {
            case NOMBRE:
                campoNombre.setError(error.errorCampo);
                break;
            case TELEFONO:
                campoTelefono.setError(error.errorCampo);
                break;
            case FOTO:
                break;
        }
        Alertas.mostrar(this, error.titulo.startsWith("Falta") ? Alertas.Tipo.ADVERTENCIA : Alertas.Tipo.ERROR,
                error.titulo, error.mensaje);
    }

    private void mostrarCarga(boolean visible) {
        capaCargando.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** Deja listo el formulario para otro contacto (la ubicacion se conserva: sigue siendo la actual). */
    private void limpiarFormulario() {
        txtNombre.setText("");
        txtTelefono.setText("");
        campoNombre.setError(null);
        campoTelefono.setError(null);
        archivoFoto = null;
        Utilidades.cargarFoto(imgFoto, null);
        txtNombre.requestFocus();
    }
}
