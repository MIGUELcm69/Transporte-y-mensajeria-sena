package com.transportes.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnvioTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 1, 15);

    private static Envio envioConIntentos(int intentosFallidos) {
        return new Envio(1, 1, 2, 1, FECHA, intentosFallidos, BigDecimal.ZERO);
    }

    @Test
    void particularSinIntentosAplicaPlusDelCuatroPorCiento() {
        Envio envio = envioConIntentos(0);
        BigDecimal precioEnvio = new BigDecimal("100.00");

        assertEquals(new BigDecimal("4.00"), envio.calcularPlus(precioEnvio, TipoCliente.PARTICULAR));
        assertEquals(new BigDecimal("104.00"), envio.calcularPrecioTotal(precioEnvio, TipoCliente.PARTICULAR));
    }

    @Test
    void particularConDosIntentosAplicaPlusDelSeisPorCientoRedondeandoHalfUp() {
        Envio envio = envioConIntentos(2);
        BigDecimal precioEnvio = new BigDecimal("12.25");

        // 12.25 × 0.06 = 0.735 → 0.74
        assertEquals(new BigDecimal("0.74"), envio.calcularPlus(precioEnvio, TipoCliente.PARTICULAR));
        assertEquals(new BigDecimal("12.99"), envio.calcularPrecioTotal(precioEnvio, TipoCliente.PARTICULAR));
    }

    @Test
    void empresaNoTienePlusAunqueHayaIntentos() {
        Envio envio = envioConIntentos(2);
        BigDecimal precioEnvio = new BigDecimal("100.00");

        assertEquals(new BigDecimal("0.00"), envio.calcularPlus(precioEnvio, TipoCliente.EMPRESA));
        assertEquals(new BigDecimal("100.00"), envio.calcularPrecioTotal(precioEnvio, TipoCliente.EMPRESA));
    }
}
