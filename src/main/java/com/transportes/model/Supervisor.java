package com.transportes.model;

import java.util.Objects;

public class Supervisor {

    private Integer id;
    private final String nif;
    private final String nombre;
    private final String direccion;
    private final String telefono;

    public Supervisor(Integer id, String nif, String nombre, String direccion, String telefono) {
        this.id = Validacion.idOpcional(id, "id");
        this.nif = Validacion.texto(nif, "NIF", Validacion.LONGITUD_DOCUMENTO);
        this.nombre = Validacion.texto(nombre, "nombre", Validacion.LONGITUD_NOMBRE);
        this.direccion = Validacion.texto(direccion, "dirección", Validacion.LONGITUD_DIRECCION);
        this.telefono = Validacion.texto(telefono, "teléfono", Validacion.LONGITUD_TELEFONO);
    }

    public void asignarId(int id) {
        this.id = Validacion.idNuevo(this.id, id);
    }

    public Integer getId() {
        return id;
    }

    public String getNif() {
        return nif;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Supervisor otro && nif.equals(otro.nif));
    }

    @Override
    public int hashCode() {
        return Objects.hash(nif);
    }
}
