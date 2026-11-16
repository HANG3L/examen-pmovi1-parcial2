package com.example.pm2e1grupo1.vista;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pm2e1grupo1.R;
import com.example.pm2e1grupo1.modelo.Contacto;
import com.example.pm2e1grupo1.red.ContactosApi;
import com.example.pm2e1grupo1.red.ErrorApi;
import com.example.pm2e1grupo1.util.Alertas;
import com.example.pm2e1grupo1.util.Utilidades;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

import okhttp3.Call;

/** VISTA - Pantalla 2: lista de contactos de la API con busqueda, actualizar y eliminar. */
public class ListaContactosActivity extends AppCompatActivity implements AdaptadorContactos.Oyente {

    private static final long ESPERA_BUSQUEDA_MS = 350;

    private RecyclerView lista;
    private TextView txtVacio;
    private EditText txtBuscar;
    private View indicadorCarga;
    private Button btnEliminar, btnActualizar;

    private AdaptadorContactos adaptador;
    private final Handler hilo = new Handler(Looper.getMainLooper());
    private Runnable busquedaPendiente;
    private Call llamadaActual;

    private final ActivityResultLauncher<Intent> lanzadorEdicion =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK) {
                    Alertas.aviso(this, Alertas.Tipo.EXITO, "Contacto actualizado");
                    cargar();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lista_contactos);
        Utilidades.ajustarBordes(findViewById(R.id.raiz));

        MaterialToolbar barra = findViewById(R.id.barra);
        barra.setNavigationOnClickListener(v -> finish());

        lista = findViewById(R.id.listaContactos);
        txtVacio = findViewById(R.id.txtVacio);
        txtBuscar = findViewById(R.id.txtBuscar);
        indicadorCarga = findViewById(R.id.cargando);
        btnEliminar = findViewById(R.id.btnEliminar);
        btnActualizar = findViewById(R.id.btnActualizar);

        adaptador = new AdaptadorContactos(this);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);

        txtBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence texto, int inicio, int cantidad, int despues) {
            }

            @Override
            public void onTextChanged(CharSequence texto, int inicio, int antes, int cantidad) {
            }

            @Override
            public void afterTextChanged(Editable texto) {
                programarBusqueda();
            }
        });

        btnEliminar.setOnClickListener(v -> pedirConfirmacionEliminar());
        btnActualizar.setOnClickListener(v -> abrirEdicion());

        cargar();
    }

    @Override
    protected void onDestroy() {
        hilo.removeCallbacksAndMessages(null);
        if (llamadaActual != null) {
            llamadaActual.cancel();
        }
        super.onDestroy();
    }

    // ---------- Lista (GET) y busqueda ----------

    /** Espera a que el usuario deje de escribir para no pedir a la API en cada letra. */
    private void programarBusqueda() {
        if (busquedaPendiente != null) {
            hilo.removeCallbacks(busquedaPendiente);
        }
        busquedaPendiente = this::cargar;
        hilo.postDelayed(busquedaPendiente, ESPERA_BUSQUEDA_MS);
    }

    private void cargar() {
        if (llamadaActual != null) {
            llamadaActual.cancel();
        }
        String buscar = txtBuscar.getText().toString().trim();
        mostrarCarga(true);

        llamadaActual = ContactosApi.listar(buscar, new ContactosApi.Respuesta<List<Contacto>>() {
            @Override
            public void alExito(List<Contacto> contactos) {
                runOnUiThread(() -> alListar(contactos, buscar));
            }

            @Override
            public void alError(ErrorApi error) {
                runOnUiThread(() -> alErrorDeApi(error, "No se pudo cargar la lista",
                        ListaContactosActivity.this::cargar));
            }
        });
    }

    private void alListar(List<Contacto> contactos, String buscar) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        mostrarCarga(false);
        adaptador.setContactos(contactos);

        if (contactos.isEmpty()) {
            txtVacio.setText(buscar.isEmpty() ? R.string.lista_vacia : R.string.sin_resultados);
            txtVacio.setVisibility(View.VISIBLE);
        } else {
            txtVacio.setVisibility(View.GONE);
        }
    }

    // ---------- Seleccion, foto y ubicacion ----------

    @Override
    public void alTocar(Contacto contacto) {
        adaptador.seleccionar(contacto);
        Alertas.confirmar(this, Alertas.Tipo.INFO, R.drawable.ic_mundo, "Acción",
                "Desea ir a la ubicación de " + contacto.getNombre() + "?",
                getString(R.string.si), getString(R.string.no),
                () -> irALaUbicacion(contacto));
    }

    @Override
    public void alTocarFoto(Contacto contacto) {
        Alertas.verFoto(this, contacto.getNombre(), contacto.getArchivoUrl());
    }

    /** El mapa (pantalla 3) se agrega en el ultimo paso del examen. */
    private void irALaUbicacion(Contacto contacto) {
        Alertas.mostrar(this, Alertas.Tipo.INFO, "Próximamente",
                "El mapa con la ubicación de " + contacto.getNombre() + " se agrega en el último paso.");
    }

    // ---------- Actualizar (PUT) ----------

    private void abrirEdicion() {
        Contacto contacto = adaptador.seleccionado();
        if (contacto == null) {
            avisarSinSeleccion();
            return;
        }
        Intent intento = new Intent(this, EditarContactoActivity.class)
                .putExtra(EditarContactoActivity.EXTRA_ID, contacto.getId())
                .putExtra(EditarContactoActivity.EXTRA_NOMBRE, contacto.getNombre())
                .putExtra(EditarContactoActivity.EXTRA_TELEFONO, contacto.getTelefono())
                .putExtra(EditarContactoActivity.EXTRA_LATITUD, contacto.getLatitud())
                .putExtra(EditarContactoActivity.EXTRA_LONGITUD, contacto.getLongitud())
                .putExtra(EditarContactoActivity.EXTRA_ARCHIVO_URL, contacto.getArchivoUrl());
        lanzadorEdicion.launch(intento);
    }

    // ---------- Eliminar (DELETE) ----------

    private void pedirConfirmacionEliminar() {
        Contacto contacto = adaptador.seleccionado();
        if (contacto == null) {
            avisarSinSeleccion();
            return;
        }
        Alertas.confirmar(this, Alertas.Tipo.ERROR, R.drawable.ic_eliminar, "Eliminar contacto",
                "¿Desea eliminar a " + contacto.getNombre() + "? Esta acción no se puede deshacer.",
                getString(R.string.eliminar), getString(R.string.cancelar),
                () -> eliminar(contacto));
    }

    private void eliminar(Contacto contacto) {
        mostrarCarga(true);
        llamadaActual = ContactosApi.eliminar(contacto.getId(), new ContactosApi.Respuesta<Void>() {
            @Override
            public void alExito(Void resultado) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    Alertas.aviso(ListaContactosActivity.this, Alertas.Tipo.EXITO,
                            contacto.getNombre() + " se eliminó");
                    cargar();
                });
            }

            @Override
            public void alError(ErrorApi error) {
                runOnUiThread(() -> alErrorDeApi(error, "No se pudo eliminar",
                        () -> eliminar(contacto)));
            }
        });
    }

    // ---------- Comunes ----------

    private void avisarSinSeleccion() {
        Alertas.mostrar(this, Alertas.Tipo.ADVERTENCIA, "Seleccione un contacto",
                "Toque un contacto de la lista para elegirlo.");
    }

    private void mostrarCarga(boolean visible) {
        indicadorCarga.setVisibility(visible ? View.VISIBLE : View.GONE);
        btnEliminar.setEnabled(!visible);
        btnActualizar.setEnabled(!visible);
    }

    /** Error de conectividad: mensaje con Reintentar. Error del servidor: alerta con su mensaje. */
    private void alErrorDeApi(ErrorApi error, String titulo, Runnable reintentar) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        mostrarCarga(false);

        if (error.esSinConexion()) {
            Alertas.confirmar(this, Alertas.Tipo.ERROR, R.drawable.ic_error, "Sin conexión",
                    error.getMensaje(), getString(R.string.reintentar), getString(R.string.cancelar),
                    reintentar);
        } else {
            Alertas.mostrar(this, Alertas.Tipo.ERROR, titulo, error.getMensaje());
        }
    }
}
