package com.transportes.model;

import com.transportes.exception.DatoInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidacionTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 1, 15);

    private static Envio envioConPrecioTotal(String precioTotal) {
        return new Envio(null, 1, 2, 3, FECHA, 0, new BigDecimal(precioTotal));
    }

    private static ClienteEmpresa empresaConTotalFacturado(String totalFacturado) {
        return new ClienteEmpresa(null, "CLI-100", FECHA, new BigDecimal(totalFacturado),
                "900123456-7", "Empresa S.A.S.", "Calle 10 # 20-30", "6011234567");
    }

    private static Sobre sobreConPeso(String peso) {
        return new Sobre(null, "MER-100", "Documentos", new BigDecimal("10.00"), false, false,
                1, null, null, new BigDecimal(peso));
    }

    @Test
    void elMaximoSeDerivaDePrecisionYEscala() {
        assertEquals(new BigDecimal("9999999999.99"), Validacion.PRECIO.maximo());
        assertEquals(new BigDecimal("999999999999.99"), Validacion.TOTAL_FACTURADO.maximo());
        assertEquals(new BigDecimal("9999999.999"), Validacion.PESO.maximo());
        assertEquals(new BigDecimal("999999.99"), Validacion.CILINDRADA.maximo());
    }

    @Test
    void precioEnElLimiteSeAceptaYPorEncimaSeRechaza() {
        assertDoesNotThrow(() -> envioConPrecioTotal("9999999999.99"));

        DatoInvalidoException error = assertThrows(DatoInvalidoException.class,
                () -> envioConPrecioTotal("10000000000.00"));
        assertEquals("El campo 'precio total' no puede superar 9999999999.99.", error.getMessage());
    }

    @Test
    void precioQueAlRedondearSuperaElLimiteSeRechaza() {
        assertEquals(new BigDecimal("9999999999.99"), envioConPrecioTotal("9999999999.994").getPrecioTotal());
        assertThrows(DatoInvalidoException.class, () -> envioConPrecioTotal("9999999999.995"));
    }

    @Test
    void totalFacturadoEnElLimiteSeAceptaYPorEncimaSeRechaza() {
        assertDoesNotThrow(() -> empresaConTotalFacturado("999999999999.99"));
        assertThrows(DatoInvalidoException.class, () -> empresaConTotalFacturado("1000000000000.00"));
    }

    @Test
    void pesoEnElLimiteSeAceptaYPorEncimaSeRechaza() {
        assertDoesNotThrow(() -> sobreConPeso("9999999.999"));
        assertThrows(DatoInvalidoException.class, () -> sobreConPeso("10000000.000"));
    }
}
