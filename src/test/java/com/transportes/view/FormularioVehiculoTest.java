package com.transportes.view;

import com.transportes.model.Avion;
import com.transportes.model.Furgoneta;
import com.transportes.model.Moto;
import com.transportes.model.TipoVehiculo;
import com.transportes.model.Vehiculo;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Scanner;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormularioVehiculoTest {

    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();

    private FormularioVehiculo formulario(String... lineas) {
        return new FormularioVehiculo(new Consola(new Scanner(String.join("\n", lineas) + "\n"),
                new PrintStream(salida, true, StandardCharsets.UTF_8)));
    }

    @Test
    void elegirTipoOfreceLosTresTiposYCeroCancela() {
        assertEquals(TipoVehiculo.FURGONETA, formulario("2").elegirTipo());
        assertTrue(salida.toString(StandardCharsets.UTF_8).contains("3. AVION"));
        assertThrows(OperacionCancelada.class, () -> formulario("0").elegirTipo());
    }

    @Test
    void motoNuevaConSuCilindradaYUnObjetoDistintoEnCadaLlamada() {
        IntFunction<Vehiculo> fabrica = formulario("M-1", "Moto de reparto", "15/03/2025", "1500,50", "125")
                .pedirNuevo(TipoVehiculo.MOTO);

        Vehiculo moto = fabrica.apply(7);
        Moto otra = (Moto) fabrica.apply(7);

        assertInstanceOf(Moto.class, moto);
        assertNull(moto.getId());
        assertEquals("M-1", moto.getCodigo());
        assertEquals(7, moto.getSupervisorId());
        assertEquals(LocalDate.of(2025, 3, 15), moto.getFechaCompra());
        assertEquals(new BigDecimal("1500.50"), moto.getPrecioCompra());
        assertEquals(new BigDecimal("125"), otra.getCilindrada());
        assertNotSame(moto, otra);
    }

    @Test
    void furgonetaYAvionPidenSoloSusDatos() {
        Vehiculo furgoneta = formulario("F-1", "Furgoneta", "01/02/2024", "20000", "3,5")
                .pedirNuevo(TipoVehiculo.FURGONETA).apply(1);
        Vehiculo avion = formulario("A-1", "Avión de carga", "01/02/2024", "900000")
                .pedirNuevo(TipoVehiculo.AVION).apply(1);

        assertEquals(new BigDecimal("3.5"), ((Furgoneta) furgoneta).getCapacidad());
        assertInstanceOf(Avion.class, avion);
    }

    @Test
    void modificarConEnterConservaLosDatosYCambiaElSupervisor() {
        Moto actual = new Moto(4, "M-1", "Moto de reparto", LocalDate.of(2025, 3, 15),
                new BigDecimal("1500.00"), 7, new BigDecimal("125.00"));

        Vehiculo modificado = formulario("", "", "1400", "").pedirModificado(actual, 9);

        assertInstanceOf(Moto.class, modificado);
        assertNotSame(actual, modificado);
        assertEquals(4, modificado.getId());
        assertEquals("M-1", modificado.getCodigo());
        assertEquals("Moto de reparto", modificado.getDescripcion());
        assertEquals(LocalDate.of(2025, 3, 15), modificado.getFechaCompra());
        assertEquals(new BigDecimal("1400.00"), modificado.getPrecioCompra());
        assertEquals(9, modificado.getSupervisorId());
        assertEquals(new BigDecimal("125.00"), ((Moto) modificado).getCilindrada());
    }

    @Test
    void resumenIncluyeElDatoPropioDelTipo() {
        Moto moto = new Moto(1, "M-1", "Moto", LocalDate.of(2025, 3, 15), new BigDecimal("1500"), 7,
                new BigDecimal("125.00"));

        assertEquals("M-1 · MOTO · Moto · compra 15/03/2025 · precio 1500.00 · cilindrada 125",
                FormularioVehiculo.resumen(moto));
    }
}
