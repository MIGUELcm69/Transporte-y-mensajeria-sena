package com.transportes.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Furgoneta extends Vehiculo {

    private BigDecimal capacidad;

    public Furgoneta(Integer id, String codigo, String descripcion, LocalDate fechaCompra,
                     BigDecimal precioCompra, int supervisorId, BigDecimal capacidad) {
        super(id, codigo, TipoVehiculo.FURGONETA, descripcion, fechaCompra, precioCompra, supervisorId);
        setCapacidad(capacidad);
    }

    public BigDecimal getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(BigDecimal capacidad) {
        this.capacidad = Validacion.positivo(capacidad, "capacidad");
    }
}
