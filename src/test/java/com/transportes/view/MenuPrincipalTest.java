package com.transportes.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuPrincipalTest {

    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private final Logger log = Logger.getLogger(ManejadorErrores.class.getName());
    private int ejecuciones;

    @BeforeEach
    void silenciarLog() {
        log.setUseParentHandlers(false);
    }

    @AfterEach
    void restaurarLog() {
        log.setUseParentHandlers(true);
    }

    /** Un módulo con una opción que cuenta sus ejecuciones y otra que falla de forma inesperada. */
    private final class ModuloDePrueba extends VistaModulo {

        ModuloDePrueba(Consola consola, ManejadorErrores manejador) {
            super(consola, manejador);
        }

        @Override
        public String titulo() {
            return "Prueba";
        }

        @Override
        protected List<Opcion> opciones() {
            return List.of(
                    new Opcion("Contar", () -> ejecuciones++),
                    new Opcion("Fallar", () -> {
                        throw new IllegalStateException("fallo");
                    }));
        }
    }

    private void ejecutar(String... lineas) {
        Consola consola = new Consola(new Scanner(String.join("\n", lineas) + "\n"),
                new PrintStream(salida, true, StandardCharsets.UTF_8));
        ManejadorErrores manejador = new ManejadorErrores(consola);
        new MenuPrincipal(consola, manejador, List.of(new ModuloDePrueba(consola, manejador))).iniciar();
    }

    private String salida() {
        return salida.toString(StandardCharsets.UTF_8);
    }

    @Test
    void unErrorEnUnaOpcionNoCierraElModuloNiLaAplicacion() {
        // Entrar al módulo, fallar, contar, volver y salir.
        ejecutar("1", "2", "1", "0", "0");

        assertEquals(1, ejecuciones);
        assertTrue(salida().contains(ManejadorErrores.MENSAJE_INESPERADO));
        assertTrue(salida().contains("Hasta pronto."));
    }

    @Test
    void entradaAgotadaTerminaSinError() {
        ejecutar("1", "1");

        assertEquals(1, ejecuciones);
        assertTrue(salida().contains("Hasta pronto."));
    }
}
