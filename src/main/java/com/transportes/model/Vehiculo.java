package com.transportes.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public abstract class Vehiculo {

    private Integer id;
    private final String codigo;
    private final TipoVehiculo tipo;
    private String descripcion;
    private LocalDate fechaCompra;
    private BigDecimal precioCompra;
    private int supervisorId;

    protected Vehiculo(Integer id, String codigo, TipoVehiculo tipo, String descripcion,
                       LocalDate fechaCompra, BigDecimal precioCompra, int supervisorId) {
        this.id = Validacion.idOpcional(id, "id");
        this.codigo = Validacion.texto(codigo, "código", Validacion.LONGITUD_CODIGO);
        this.tipo = Validacion.obligatorio(tipo, "tipo");
        setDescripcion(descripcion);
        setFechaCompra(fechaCompra);
        setPrecioCompra(precioCompra);
        setSupervisorId(supervisorId);
    }

    public void asignarId(int id) {
        this.id = Validacion.idNuevo(this.id, id);
    }

    public Integer getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = Validacion.texto(descripcion, "descripción", Validacion.LONGITUD_DESCRIPCION);
    }

    public LocalDate getFechaCompra() {
        return fechaCompra;
    }

    public void setFechaCompra(LocalDate fechaCompra) {
        this.fechaCompra = Validacion.obligatorio(fechaCompra, "fecha de compra");
    }

    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompra = Validacion.importe(precioCompra, "precio de compra");
    }

    public int getSupervisorId() {
        return supervisorId;
    }

    public void setSupervisorId(int supervisorId) {
        this.supervisorId = Validacion.id(supervisorId, "supervisor");
    }

    @Override
    public final boolean equals(Object o) {
        return this == o || (o instanceof Vehiculo otro && codigo.equals(otro.codigo));
    }

    @Override
    public final int hashCode() {
        return Objects.hash(codigo);
    }
}
