package com.example.pm2e1grupo1.controlador;

import android.content.Context;

import com.example.pm2e1grupo1.dao.BaseDatos;
import com.example.pm2e1grupo1.dao.PaisDao;

import java.util.List;

/** CONTROLADOR: lista de paises disponibles en el formulario. */
public class PaisControlador {

    private final PaisDao dao;

    public PaisControlador(Context contexto) {
        this.dao = new PaisDao(new BaseDatos(contexto));
    }

    public List<String> listar() {
        return dao.listar();
    }

    /** Devuelve false si el pais ya estaba registrado. */
    public boolean agregar(String nombrePais) {
        return dao.insertar(nombrePais.trim());
    }
}
