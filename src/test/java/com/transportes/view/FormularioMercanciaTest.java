package com.transportes.view;

import com.transportes.model.Mercancia;
import com.transportes.model.Paquete;
import com.transportes.model.Sobre;
import com.transportes.model.TipoMercancia;
import com.transportes.model.TipoVehiculo;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormularioMercanciaTest {

    private static final Map<TipoVehiculo, Integer> ID_POR_TIPO =
            Map.of(TipoVehiculo.MOTO, 1, TipoVehiculo.FURGONETA, 2, TipoVehiculo.AVION, 3);

    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    /** Cada elección del selector falso: "etiqueta:TIPO:actualId". */
    private final List<String> elecciones = new ArrayList<>();

    /** El selector falso devuelve el id actual si lo hay; si no, uno fijo por tipo. */
    private FormularioMercancia formulario(String... lineas) {
        return new FormularioMercancia(new Consola(new Scanner(String.join("\n", lineas) + "\n"),
                new PrintStream(salida, true, StandardCharsets.UTF_8)),
                (etiqueta, tipo, actualId) -> {
                    elecciones.add(etiqueta + ":" + tipo + ":" + actualId);
                    return actualId != null ? actualId : ID_POR_TIPO.get(tipo);
                });
    }

    @Test
    void elegirTipoOfreceSobreYPaqueteYCeroCancela() {
        assertEquals(TipoMercancia.PAQUETE, formulario("2").elegirTipo());
        assertTrue(salida.toString(StandardCharsets.UTF_8).contains("1. SOBRE"));
        assertThrows(OperacionCancelada.class, () -> formulario("0").elegirTipo());
    }

    @Test
    void sobreSinLargoRecorridoPideSoloSuPesoYUnaMoto() {
        FormularioMercancia formulario = formulario("S-1", "Contrato", "12,50", "s", "n", "0,250",
                "S-1", "Contrato", "12,50", "s", "n", "0,250");

        Mercancia sobre = formulario.pedirNueva(TipoMercancia.SOBRE);
        Mercancia otro = formulario.pedirNueva(TipoMercancia.SOBRE);

        assertInstanceOf(Sobre.class, sobre);
        assertNull(sobre.getId());
        assertEquals("S-1", sobre.getCodigo());
        assertEquals(new BigDecimal("12.50"), sobre.getPrecioEnvio());
        assertTrue(sobre.isAsegurada());
        assertFalse(sobre.isLargoRecorrido());
        assertEquals(new BigDecimal("0.250"), ((Sobre) sobre).getPeso());
        assertEquals(1, sobre.getVehiculoId());
        assertNull(sobre.getAvionId());
        assertEquals(List.of("Vehículo:MOTO:null", "Vehículo:MOTO:null"), elecciones);
        assertNotSame(sobre, otro);
    }

    @Test
    void paqueteDeLargoRecorridoPideSusMedidasFurgonetaAvionYFurgonetaDeDestino() {
        Mercancia paquete = formulario("P-1", "Caja", "30", "n", "s", "40", "30", "20,5")
                .pedirNueva(TipoMercancia.PAQUETE);

        Paquete leido = (Paquete) paquete;
        assertEquals(new BigDecimal("40"), leido.getLargo());
        assertEquals(new BigDecimal("30"), leido.getAncho());
        assertEquals(new BigDecimal("20.5"), leido.getAlto());
        assertTrue(paquete.isLargoRecorrido());
        assertEquals(2, paquete.getVehiculoId());
        assertEquals(3, paquete.getAvionId());
        assertEquals(2, paquete.getVehiculoDestinoId());
        assertEquals(List.of("Vehículo:FURGONETA:null", "Avión:AVION:null", "Vehículo de destino:FURGONETA:null"),
                elecciones);
    }

    @Test
    void modificarConEnterConservaLosDatosYLosVehiculos() {
        Sobre actual = new Sobre(5, "S-1", "Contrato", new BigDecimal("12.50"), true, true, 7, 8, 9,
                new BigDecimal("0.250"));

        Mercancia modificado = formulario("", "", "", "", "").pedirModificada(actual, true);

        assertNotSame(actual, modificado);
        assertEquals(5, modificado.getId());
        assertEquals("S-1", modificado.getCodigo());
        assertEquals("Contrato", modificado.getDescripcion());
        assertEquals(new BigDecimal("12.50"), modificado.getPrecioEnvio());
        assertTrue(modificado.isAsegurada());
        assertTrue(modificado.isLargoRecorrido());
        assertEquals(new BigDecimal("0.250"), ((Sobre) modificado).getPeso());
        assertEquals(List.of("Vehículo:MOTO:7", "Avión:AVION:8", "Vehículo de destino:MOTO:9"), elecciones);
    }

    @Test
    void modificarConPrecioBloqueadoNoLoPreguntaYLoConserva() {
        Sobre actual = new Sobre(5, "S-1", "Contrato", new BigDecimal("12.50"), false, false, 7, null, null,
                new BigDecimal("0.250"));

        // Sin pregunta de precio: descripción, asegurada, largo recorrido y peso.
        Mercancia modificado = formulario("Contrato firmado", "s", "", "").pedirModificada(actual, false);

        assertEquals("Contrato firmado", modificado.getDescripcion());
        assertEquals(new BigDecimal("12.50"), modificado.getPrecioEnvio());
        assertTrue(modificado.isAsegurada());
        assertFalse(salida.toString(StandardCharsets.UTF_8).contains("Precio de envío"));
    }

    @Test
    void modificarAPasarALargoRecorridoPideAvionYDestinoSinValorActual() {
        Paquete actual = new Paquete(5, "P-1", "Caja", new BigDecimal("30.00"), false, false, 7, null, null,
                new BigDecimal("40"), new BigDecimal("30"), new BigDecimal("20"));

        Mercancia modificado = formulario("", "", "", "s", "", "", "").pedirModificada(actual, true);

        assertTrue(modificado.isLargoRecorrido());
        assertEquals(7, modificado.getVehiculoId());
        assertEquals(List.of("Vehículo:FURGONETA:7", "Avión:AVION:null", "Vehículo de destino:FURGONETA:null"),
                elecciones);
    }

    @Test
    void resumenYTramosMuestranLoPropioDelTipo() {
        Paquete paquete = new Paquete(1, "P-1", "Caja", new BigDecimal("30"), true, true, 7, 8, 9,
                new BigDecimal("40.00"), new BigDecimal("30.00"), new BigDecimal("20.50"));
        Sobre sobre = new Sobre(2, "S-1", "Contrato", new BigDecimal("12.5"), false, false, 7, null, null,
                new BigDecimal("0.250"));

        assertEquals("P-1 · PAQUETE · Caja · precio 30.00 · asegurada sí · largo recorrido sí · medidas 40 x 30 x 20.5",
                FormularioMercancia.resumen(paquete));
        assertEquals("S-1 · SOBRE · Contrato · precio 12.50 · asegurada no · largo recorrido no · peso 0.25",
                FormularioMercancia.resumen(sobre));
        assertEquals("vehículo V7 · avión V8 · vehículo de destino V9",
                FormularioMercancia.tramos(paquete, id -> "V" + id));
        assertEquals("vehículo V7", FormularioMercancia.tramos(sobre, id -> "V" + id));
    }
}
