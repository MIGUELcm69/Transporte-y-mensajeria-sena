package com.transportes.view;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsolaTest {

    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();

    /** Cada argumento es una línea que "escribe" el usuario. */
    private Consola consola(String... lineas) {
        String texto = lineas.length == 0 ? "" : String.join("\n", lineas) + "\n";
        return new Consola(new Scanner(texto), new PrintStream(salida, true, StandardCharsets.UTF_8));
    }

    private String salida() {
        return salida.toString(StandardCharsets.UTF_8);
    }

    private int errores() {
        return salida().split("\\[ERROR]", -1).length - 1;
    }

    // ----- Enteros y menús -----

    @Test
    void leerEnteroVuelveAPreguntarAnteTextoYFueraDeRango() {
        assertEquals(5, consola("abc", "99", "", "5").leerEntero("Cantidad", 1, 10));
        assertEquals(3, errores());
        assertTrue(salida().contains("entre 1 y 10"));
    }

    @Test
    void leerEnteroRechazaUnNumeroQueNoCabeEnInt() {
        assertEquals(7, consola("99999999999", "7").leerEntero("Cantidad", 0, 10));
        assertEquals(1, errores());
    }

    @Test
    void elegirOpcionMuestraLasOpcionesYNoTrataCancelarComoPalabraClave() {
        int elegida = consola("cancelar", "3", "2").elegirOpcion("Vehículos", List.of("Registrar", "Listar"), "Volver");

        assertEquals(2, elegida);
        assertEquals(2, errores());
        assertTrue(salida().contains("1. Registrar"));
        assertTrue(salida().contains("0. Volver"));
    }

    // ----- Decimales -----

    @Test
    void leerDecimalAceptaComaYPunto() {
        assertEquals(new BigDecimal("12.50"), consola("12,50").leerDecimal("Precio", Consola.Rango.NO_NEGATIVO));
        assertEquals(new BigDecimal("3.5"), consola("3.5").leerDecimal("Precio", Consola.Rango.NO_NEGATIVO));
    }

    @Test
    void leerDecimalRechazaFormatosInvalidos() {
        BigDecimal valor = consola("abc", "1.000,50", "12,", "", "8").leerDecimal("Precio", Consola.Rango.NO_NEGATIVO);

        assertEquals(new BigDecimal("8"), valor);
        assertEquals(4, errores());
    }

    @Test
    void rangoPositivoRechazaNegativoYCero() {
        assertEquals(new BigDecimal("2"), consola("-1", "0", "2").leerDecimal("Peso", Consola.Rango.POSITIVO));
        assertEquals(2, errores());
    }

    @Test
    void rangoNoNegativoAdmiteCeroYRechazaNegativo() {
        assertEquals(BigDecimal.ZERO, consola("-3", "0").leerDecimal("Precio", Consola.Rango.NO_NEGATIVO));
        assertEquals(1, errores());
    }

    // ----- Fechas -----

    @Test
    void leerFechaEsEstrictaYUsaDiaMesAnio() {
        LocalDate fecha = consola("31/02/2026", "2026-03-15", "15/3/2026", "15/03/2026").leerFecha("Fecha de compra");

        assertEquals(LocalDate.of(2026, 3, 15), fecha);
        assertEquals(3, errores());
    }

    @Test
    void leerFechaNoPoneValorPorDefectoConEnter() {
        assertEquals(LocalDate.of(2026, 1, 1), consola("", "01/01/2026").leerFecha("Fecha de compra"));
        assertEquals(1, errores());
    }

    @Test
    void leerFechaOHoyUsaHoyConEnterYLoMuestra() {
        LocalDate hoy = LocalDate.of(2026, 11, 2);

        assertEquals(hoy, consola("").leerFechaOHoy("Fecha de envío", hoy));
        assertTrue(salida().contains("Se usa la fecha de hoy: 02/11/2026."));
    }

    @Test
    void leerFechaOHoyAceptaOtraFecha() {
        assertEquals(LocalDate.of(2026, 10, 30),
                consola("30/10/2026").leerFechaOHoy("Fecha de envío", LocalDate.of(2026, 11, 2)));
    }

    // ----- Textos y confirmación -----

    @Test
    void leerTextoNoVacioRecortaYVuelveAPreguntarSiEstaVacio() {
        assertEquals("Ana Pérez", consola("", "   ", "  Ana Pérez  ").leerTextoNoVacio("Nombre"));
        assertEquals(2, errores());
    }

    @Test
    void confirmarAceptaSiYNoEnVariasFormas() {
        assertTrue(consola("quizá", "Sí").confirmar("¿Eliminar?"));
        assertEquals(1, errores());
        assertFalse(consola("N").confirmar("¿Eliminar?"));
        assertTrue(consola("si").confirmar("¿Eliminar?"));
    }

    // ----- Cancelar y fin de entrada -----

    @Test
    void cancelarEnUnCampoLanzaOperacionCancelada() {
        assertThrows(OperacionCancelada.class, () -> consola("CANCELAR").leerTextoNoVacio("Nombre"));
        assertThrows(OperacionCancelada.class, () -> consola("abc", "cancelar").leerEntero("Cantidad", 1, 9));
        assertThrows(OperacionCancelada.class, () -> consola("cancelar").confirmar("¿Seguro?"));
    }

    @Test
    void entradaAgotadaLanzaFinDeEntrada() {
        assertThrows(FinDeEntrada.class, () -> consola().leerTextoNoVacio("Nombre"));
        assertThrows(FinDeEntrada.class, () -> consola("abc").leerEntero("Cantidad", 1, 9));
        assertThrows(FinDeEntrada.class, () -> consola("9").elegirOpcion("Menú", List.of("Uno"), "Salir"));
    }

    // ----- Modificar: Enter conserva el valor actual -----

    @Test
    void enterConservaElValorActual() {
        assertEquals("Ana", consola("").leerTextoOActual("Nombre", "Ana"));
        assertEquals(new BigDecimal("10.50"),
                consola("").leerDecimalOActual("Precio", Consola.Rango.NO_NEGATIVO, new BigDecimal("10.50")));
        assertEquals(LocalDate.of(2025, 5, 1), consola("").leerFechaOActual("Fecha", LocalDate.of(2025, 5, 1)));
        assertTrue(consola("").confirmarOActual("¿Asegurada?", true));
        assertTrue(salida().contains("[Ana, Enter = conservar]"));
    }

    @Test
    void lasVariantesConActualAceptanUnValorNuevoYLoValidan() {
        assertEquals("Luis", consola("Luis").leerTextoOActual("Nombre", "Ana"));
        assertEquals(new BigDecimal("2"),
                consola("-1", "2").leerDecimalOActual("Peso", Consola.Rango.POSITIVO, BigDecimal.ONE));
        assertEquals(LocalDate.of(2026, 2, 28),
                consola("29/02/2026", "28/02/2026").leerFechaOActual("Fecha", LocalDate.of(2025, 5, 1)));
        assertFalse(consola("n").confirmarOActual("¿Asegurada?", true));
        assertEquals(2, errores());
    }
}
