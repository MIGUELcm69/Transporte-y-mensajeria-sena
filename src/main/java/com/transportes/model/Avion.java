package com.transportes.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Avion extends Vehiculo {

    public Avion(Integer id, String codigo, String descripcion, LocalDate fechaCompra,
                 BigDecimal precioCompra, int supervisorId) {
        super(id, codigo, TipoVehiculo.AVION, descripcion, fechaCompra, precioCompra, supervisorId);
    }
}
