package com.example.pm2e1grupo1.vista;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pm2e1grupo1.R;
import com.example.pm2e1grupo1.modelo.Contacto;
import com.example.pm2e1grupo1.red.CargadorImagenes;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lista de contactos con un elemento seleccionado (resaltado) y la foto descargada de la API. */
public class AdaptadorContactos extends RecyclerView.Adapter<AdaptadorContactos.Fila> {

    public interface Oyente {
        /** Se toco la fila (el contacto queda seleccionado). */
        void alTocar(Contacto contacto);

        /** Se toco la fotografia: se muestra en grande. */
        void alTocarFoto(Contacto contacto);
    }

    private final Oyente oyente;
    private final List<Contacto> contactos = new ArrayList<>();
    private int idSeleccionado = -1;

    public AdaptadorContactos(Oyente oyente) {
        this.oyente = oyente;
    }

    public void setContactos(List<Contacto> nuevos) {
        contactos.clear();
        contactos.addAll(nuevos);
        if (seleccionado() == null) {
            idSeleccionado = -1; // el contacto seleccionado ya no esta en la lista
        }
        notifyDataSetChanged();
    }

    public void seleccionar(Contacto contacto) {
        int anterior = posicionDe(idSeleccionado);
        idSeleccionado = contacto.getId();
        if (anterior >= 0) {
            notifyItemChanged(anterior);
        }
        notifyItemChanged(posicionDe(idSeleccionado));
    }

    /** Contacto seleccionado, o null si no hay ninguno. */
    public Contacto seleccionado() {
        int posicion = posicionDe(idSeleccionado);
        return posicion >= 0 ? contactos.get(posicion) : null;
    }

    private int posicionDe(int id) {
        for (int i = 0; i < contactos.size(); i++) {
            if (contactos.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        return new Fila(LayoutInflater.from(padre.getContext()).inflate(R.layout.item_contacto, padre, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Fila fila, int posicion) {
        Contacto contacto = contactos.get(posicion);

        fila.nombre.setText(contacto.getNombre());
        fila.telefono.setText(formatearTelefono(contacto.getTelefono()));
        fila.ubicacion.setText(String.format(Locale.US, "Lat %.4f · Lng %.4f",
                contacto.getLatitud(), contacto.getLongitud()));

        CargadorImagenes.mostrar(fila.avatar, contacto.getArchivoUrl(), 160);

        boolean elegido = contacto.getId() == idSeleccionado;
        int color = MaterialColors.getColor(fila.tarjeta, elegido
                ? com.google.android.material.R.attr.colorPrimaryContainer
                : com.google.android.material.R.attr.colorSurface);
        fila.tarjeta.setCardBackgroundColor(color);

        fila.tarjeta.setOnClickListener(v -> oyente.alTocar(contacto));
        fila.avatar.setOnClickListener(v -> oyente.alTocarFoto(contacto));
    }

    @Override
    public int getItemCount() {
        return contactos.size();
    }

    /** 99765645 -> 9976-5645 */
    static String formatearTelefono(String telefono) {
        if (telefono != null && telefono.length() == 8) {
            return telefono.substring(0, 4) + "-" + telefono.substring(4);
        }
        return telefono;
    }

    static class Fila extends RecyclerView.ViewHolder {
        final MaterialCardView tarjeta;
        final ImageView avatar;
        final TextView nombre, telefono, ubicacion;

        Fila(@NonNull View vista) {
            super(vista);
            tarjeta = vista.findViewById(R.id.tarjeta);
            avatar = vista.findViewById(R.id.imgAvatar);
            nombre = vista.findViewById(R.id.txtNombre);
            telefono = vista.findViewById(R.id.txtTelefono);
            ubicacion = vista.findViewById(R.id.txtUbicacion);
        }
    }
}
