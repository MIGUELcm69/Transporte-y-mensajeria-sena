package com.transportes.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public abstract class Cliente {

    private Integer id;
    private final String codigoCliente;
    private final TipoCliente tipo;
    private final LocalDate fechaAlta;
    // Sin setter: solo cambia en BD con UPDATE incremental (RF-17).
    private final BigDecimal totalFacturado;
    private String nombre;
    private String direccion;

    protected Cliente(Integer id, String codigoCliente, TipoCliente tipo, LocalDate fechaAlta,
                      BigDecimal totalFacturado, String nombre, String direccion) {
        this.id = Validacion.idOpcional(id, "id");
        this.codigoCliente = Validacion.texto(codigoCliente, "código de cliente", Validacion.LONGITUD_CODIGO);
        this.tipo = Validacion.obligatorio(tipo, "tipo");
        this.fechaAlta = Validacion.obligatorio(fechaAlta, "fecha de alta");
        this.totalFacturado = Validacion.importe(totalFacturado, "total facturado");
        setNombre(nombre);
        setDireccion(direccion);
    }

    public void asignarId(int id) {
        this.id = Validacion.idNuevo(this.id, id);
    }

    public Integer getId() {
        return id;
    }

    public String getCodigoCliente() {
        return codigoCliente;
    }

    public TipoCliente getTipo() {
        return tipo;
    }

    public LocalDate getFechaAlta() {
        return fechaAlta;
    }

    public BigDecimal getTotalFacturado() {
        return totalFacturado;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = Validacion.texto(nombre, "nombre", Validacion.LONGITUD_NOMBRE);
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = Validacion.texto(direccion, "dirección", Validacion.LONGITUD_DIRECCION);
    }

    @Override
    public final boolean equals(Object o) {
        return this == o || (o instanceof Cliente otro && codigoCliente.equals(otro.codigoCliente));
    }

    @Override
    public final int hashCode() {
        return Objects.hash(codigoCliente);
    }
}
