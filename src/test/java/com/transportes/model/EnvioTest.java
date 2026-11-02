package com.transportes.model;

import com.transportes.exception.DatoInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void conDatosConservaIdEIntentosYRecalculaElPlusConEsosIntentos() {
        Envio original = new Envio(7, 1, 2, 1, FECHA, 2, new BigDecimal("12.99"));

        Envio modificado = original.conDatos(3, 2, 5, FECHA.plusDays(1), new BigDecimal("100.00"),
                TipoCliente.PARTICULAR);

        assertEquals(7, modificado.getId());
        assertEquals(2, modificado.getIntentosFallidos());
        assertEquals(3, modificado.getClienteOrigenId());
        assertEquals(5, modificado.getMercanciaId());
        assertEquals(FECHA.plusDays(1), modificado.getFechaEnvio());
        // 100.00 × (0.04 + 0.01 × 2) = 6.00
        assertEquals(new BigDecimal("106.00"), modificado.getPrecioTotal());
    }

    @Test
    void conDatosRechazaDestinatarioEmpresaSiYaHayIntentosFallidos() {
        Envio original = envioConIntentos(1);

        DatoInvalidoException error = assertThrows(DatoInvalidoException.class,
                () -> original.conDatos(1, 3, 1, FECHA, new BigDecimal("100.00"), TipoCliente.EMPRESA));

        assertTrue(error.getMessage().contains("intento"), error.getMessage());
    }

    @Test
    void conDatosAEmpresaSinIntentosQuitaElPlus() {
        Envio original = envioConIntentos(0);

        Envio modificado = original.conDatos(1, 3, 1, FECHA, new BigDecimal("100.00"), TipoCliente.EMPRESA);

        assertEquals(new BigDecimal("100.00"), modificado.getPrecioTotal());
    }

    @Test
    void conDatosVuelveAValidarClientesDistintosIdsYFecha() {
        Envio original = envioConIntentos(0);
        BigDecimal precio = new BigDecimal("100.00");

        assertThrows(DatoInvalidoException.class,
                () -> original.conDatos(2, 2, 1, FECHA, precio, TipoCliente.PARTICULAR));
        assertThrows(DatoInvalidoException.class,
                () -> original.conDatos(1, 2, 0, FECHA, precio, TipoCliente.PARTICULAR));
        assertThrows(DatoInvalidoException.class,
                () -> original.conDatos(1, 2, 1, null, precio, TipoCliente.PARTICULAR));
    }
}
