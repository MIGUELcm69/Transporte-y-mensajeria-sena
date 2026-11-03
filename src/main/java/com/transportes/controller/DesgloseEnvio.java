package com.transportes.controller;

import java.math.BigDecimal;
import java.util.Objects;

/** Precio de envío de la mercancía, plus de almacenaje y precio total de un envío (RF-31, RF-32). */
public record DesgloseEnvio(BigDecimal precioBase, BigDecimal plus, BigDecimal precioTotal) {

    public DesgloseEnvio {
        Objects.requireNonNull(precioBase, "precioBase");
        Objects.requireNonNull(plus, "plus");
        Objects.requireNonNull(precioTotal, "precioTotal");
    }
}
