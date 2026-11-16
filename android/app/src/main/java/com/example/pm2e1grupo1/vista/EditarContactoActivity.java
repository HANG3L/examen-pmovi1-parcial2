package com.example.pm2e1grupo1.vista;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.ImageViewCompat;

import com.example.pm2e1grupo1.R;
import com.example.pm2e1grupo1.controlador.ContactoControlador;
import com.example.pm2e1grupo1.modelo.Contacto;
import com.example.pm2e1grupo1.red.ApiConfig;
import com.example.pm2e1grupo1.red.CargadorImagenes;
import com.example.pm2e1grupo1.red.ContactosApi;
import com.example.pm2e1grupo1.red.ErrorApi;
import com.example.pm2e1grupo1.util.Alertas;
import com.example.pm2e1grupo1.util.CapturaFoto;
import com.example.pm2e1grupo1.util.ImagenJpeg;
import com.example.pm2e1grupo1.util.Utilidades;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;

/**
 * VISTA - Actualizar un contacto (PUT). Muestra los datos actuales; la fotografia es opcional:
 * si no se toma una nueva, se conserva la que ya tiene el contacto en la API.
 */
public class EditarContactoActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "id";
    public static final String EXTRA_NOMBRE = "nombre";
    public static final String EXTRA_TELEFONO = "telefono";
    public static final String EXTRA_LATITUD = "latitud";
    public static final String EXTRA_LONGITUD = "longitud";
    public static final String EXTRA_ARCHIVO_URL = "archivoUrl";

    private ImageView imgFoto;
    private TextInputLayout campoNombre, campoTelefono;
    private EditText txtNombre, txtTelefono;
    private View capaCargando;

    private final ContactoControlador controlador = new ContactoControlador();
    private CapturaFoto capturaFoto;

    private int id;
    private double latitud;
    private double longitud;

    /** Fotografia nueva (null si se conserva la actual). */
    private File fotoNueva;

    private final ExecutorService trabajo = Executors.newSingleThreadExecutor();
    private Call llamadaActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editar_contacto);
        Utilidades.ajustarBordes(findViewById(R.id.raiz));

        imgFoto = findViewById(R.id.imgFoto);
        campoNombre = findViewById(R.id.campoNombre);
        campoTelefono = findViewById(R.id.campoTelefono);
        txtNombre = findViewById(R.id.txtNombre);
        txtTelefono = findViewById(R.id.txtTelefono);
        capaCargando = findViewById(R.id.cargando);

        ((MaterialToolbar) findViewById(R.id.barra))
                .setNavigationOnClickListener(v -> finish());

        capturaFoto = new CapturaFoto(this, this::alTomarFoto);

        id = getIntent().getIntExtra(EXTRA_ID, -1);
        latitud = getIntent().getDoubleExtra(EXTRA_LATITUD, 0);
        longitud = getIntent().getDoubleExtra(EXTRA_LONGITUD, 0);
        txtNombre.setText(getIntent().getStringExtra(EXTRA_NOMBRE));
        txtTelefono.setText(getIntent().getStringExtra(EXTRA_TELEFONO));

        // La ubicacion se conserva tal como esta guardada; solo se muestra.
        ((TextInputLayout) findViewById(R.id.campoLatitud)).setHelperText(null);
        ((EditText) findViewById(R.id.txtLatitud)).setText(String.format(Locale.US, "%.6f", latitud));
        ((EditText) findViewById(R.id.txtLongitud)).setText(String.format(Locale.US, "%.6f", longitud));

        mostrarFotoActual(getIntent().getStringExtra(EXTRA_ARCHIVO_URL));

        Utilidades.limpiarErrorAlEscribir(campoNombre);
        Utilidades.limpiarErrorAlEscribir(campoTelefono);

        findViewById(R.id.btnTomarFoto).setOnClickListener(v -> capturaFoto.tomar());
        findViewById(R.id.btnGuardarCambios).setOnClickListener(v -> guardar());
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

    /** Descarga y muestra la fotografia que el contacto tiene en la API. */
    private void mostrarFotoActual(String archivoUrl) {
        ImageViewCompat.setImageTintList(imgFoto, null);
        imgFoto.setPadding(0, 0, 0, 0);
        imgFoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (archivoUrl != null) {
            CargadorImagenes.mostrar(imgFoto, archivoUrl, 512);
        }
    }

    private void alTomarFoto(File foto) {
        fotoNueva = foto;
        Utilidades.cargarFoto(imgFoto, capturaFoto.uriDe(foto).toString());
    }

    // ---------- Guardar (PUT) ----------

    private void guardar() {
        String nombre = txtNombre.getText().toString().trim();
        String telefono = txtTelefono.getText().toString().trim();

        // La fotografia no es obligatoria al actualizar: el contacto ya tiene una.
        ContactoControlador.Error error = controlador.validar(true, nombre, telefono);
        if (error != null) {
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
            return;
        }
        enviar(nombre, telefono);
    }

    private void enviar(String nombre, String telefono) {
        mostrarCarga(true);
        File foto = fotoNueva;

        trabajo.execute(() -> {
            File aSubir = null;
            if (foto != null) {
                try {
                    aSubir = ImagenJpeg.ajustarAlLimite(foto, ApiConfig.TAMANO_MAXIMO_BYTES);
                } catch (IOException ex) {
                    runOnUiThread(() -> {
                        mostrarCarga(false);
                        Alertas.mostrar(this, Alertas.Tipo.ERROR, "Fotografía no válida", ex.getMessage());
                    });
                    return;
                }
            }

            llamadaActual = ContactosApi.actualizar(id, nombre, telefono, latitud, longitud, aSubir,
                    new ContactosApi.Respuesta<Contacto>() {
                        @Override
                        public void alExito(Contacto contacto) {
                            runOnUiThread(EditarContactoActivity.this::alActualizar);
                        }

                        @Override
                        public void alError(ErrorApi error) {
                            runOnUiThread(() -> alErrorDeApi(error, nombre, telefono));
                        }
                    });
        });
    }

    private void alActualizar() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        CargadorImagenes.limpiarCache(); // la foto pudo cambiar: la lista debe volver a descargarla
        setResult(RESULT_OK);
        finish();
    }

    private void alErrorDeApi(ErrorApi error, String nombre, String telefono) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        mostrarCarga(false);

        if (error.esSinConexion()) {
            Alertas.confirmar(this, Alertas.Tipo.ERROR, R.drawable.ic_error, "Sin conexión",
                    error.getMensaje(), getString(R.string.reintentar), getString(R.string.cancelar),
                    () -> enviar(nombre, telefono));
        } else {
            Alertas.mostrar(this, Alertas.Tipo.ERROR, "No se pudo actualizar", error.getMensaje());
        }
    }

    private void mostrarCarga(boolean visible) {
        capaCargando.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
