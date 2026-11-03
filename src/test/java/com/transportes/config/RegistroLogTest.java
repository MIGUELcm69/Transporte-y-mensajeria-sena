package com.transportes.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistroLogTest {

    private final Logger raiz = Logger.getLogger("");
    private Handler[] handlersOriginales;
    private Level nivelOriginal;

    @TempDir
    Path carpeta;

    @BeforeEach
    void guardarConfiguracion() {
        handlersOriginales = raiz.getHandlers();
        nivelOriginal = raiz.getLevel();
        for (Handler handler : handlersOriginales) {
            raiz.removeHandler(handler);
        }
    }

    /** Cierra el FileHandler del test (libera el archivo en Windows) y deja el log como estaba. */
    @AfterEach
    void restaurarConfiguracion() {
        for (Handler handler : raiz.getHandlers()) {
            raiz.removeHandler(handler);
            handler.close();
        }
        for (Handler handler : handlersOriginales) {
            raiz.addHandler(handler);
        }
        raiz.setLevel(nivelOriginal);
    }

    @Test
    void escribeEnUnUnicoArchivoYQuitaElConsoleHandler() throws IOException {
        raiz.addHandler(new ConsoleHandler());
        Path archivo = carpeta.resolve("logs").resolve("transportes.log");

        assertTrue(RegistroLog.configurar(archivo));
        assertEquals(1, raiz.getHandlers().length);
        assertInstanceOf(FileHandler.class, raiz.getHandlers()[0]);

        Logger.getLogger("prueba").severe("mensaje de prueba");
        raiz.getHandlers()[0].flush();
        String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
        assertTrue(contenido.contains("SEVERE prueba - mensaje de prueba"), contenido);
    }

    @Test
    void escribeSevereEInfoEnElArchivoYNadaEnLaConsola() throws IOException {
        PrintStream outOriginal = System.out;
        PrintStream errOriginal = System.err;
        ByteArrayOutputStream consola = new ByteArrayOutputStream();
        PrintStream capturada = new PrintStream(consola, true, StandardCharsets.UTF_8);
        Path archivo = carpeta.resolve("logs").resolve("transportes.log");
        try {
            System.setOut(capturada);
            System.setErr(capturada);
            // Creado tras redirigir System.err, para que escribiera en la captura si siguiera activo.
            raiz.addHandler(new ConsoleHandler());

            assertTrue(RegistroLog.configurar(archivo));
            Logger prueba = Logger.getLogger("prueba.consola");
            prueba.severe("registro grave");
            prueba.info("registro informativo");
            raiz.getHandlers()[0].flush();
        } finally {
            System.setOut(outOriginal);
            System.setErr(errOriginal);
        }

        String contenido = Files.readString(archivo, StandardCharsets.UTF_8);
        assertTrue(contenido.contains("SEVERE prueba.consola - registro grave"), contenido);
        assertTrue(contenido.contains("INFO prueba.consola - registro informativo"), contenido);
        assertEquals("", consola.toString(StandardCharsets.UTF_8));
    }

    @Test
    void devuelveFalseSiNoPuedeAbrirElArchivoYNoDejaConsoleHandler() throws IOException {
        raiz.addHandler(new ConsoleHandler());
        Path noEsCarpeta = Files.writeString(carpeta.resolve("ocupado"), "x");

        assertFalse(RegistroLog.configurar(noEsCarpeta.resolve("transportes.log")));
        assertEquals(0, raiz.getHandlers().length);
    }

    @Test
    void elFormatoNoEscribeNiElMensajeNiLaTrazaDeLaExcepcionAdjunta() {
        LogRecord registro = new LogRecord(Level.WARNING, "Error SQL al guardar supervisor [código 1062]");
        registro.setLoggerName("dao");
        registro.setThrown(new SQLException("Duplicate entry '12345678A' for key 'uq_supervisor_nif'"));

        String linea = new RegistroLog.FormatoLinea().format(registro);

        assertTrue(linea.contains("WARNING dao - Error SQL al guardar supervisor [código 1062] [SQLException]"), linea);
        assertFalse(linea.contains("12345678A"));
        assertFalse(linea.contains("\tat "));
        assertEquals(1, linea.lines().count());
    }
}
