package com.transportes.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Moto extends Vehiculo {

    private BigDecimal cilindrada;

    public Moto(Integer id, String codigo, String descripcion, LocalDate fechaCompra,
                BigDecimal precioCompra, int supervisorId, BigDecimal cilindrada) {
        super(id, codigo, TipoVehiculo.MOTO, descripcion, fechaCompra, precioCompra, supervisorId);
        setCilindrada(cilindrada);
    }

    public BigDecimal getCilindrada() {
        return cilindrada;
    }

    public void setCilindrada(BigDecimal cilindrada) {
        this.cilindrada = Validacion.positivo(cilindrada, "cilindrada");
    }
}
