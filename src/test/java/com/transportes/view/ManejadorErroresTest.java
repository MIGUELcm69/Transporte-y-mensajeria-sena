package com.transportes.view;

import com.transportes.exception.AccesoDatosException;
import com.transportes.exception.ConfiguracionException;
import com.transportes.exception.EntidadNoEncontradaException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManejadorErroresTest {

    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();
    private final ManejadorErrores manejador = new ManejadorErrores(
            new Consola(new Scanner(""), new PrintStream(salida, true, StandardCharsets.UTF_8)));

    private final Logger log = Logger.getLogger(ManejadorErrores.class.getName());
    private final List<LogRecord> registros = new ArrayList<>();
    private final Handler captura = new Handler() {
        @Override
        public void publish(LogRecord registro) {
            registros.add(registro);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };
    private boolean usabaHandlersDelPadre;

    @BeforeEach
    void capturarLog() {
        usabaHandlersDelPadre = log.getUseParentHandlers();
        log.setUseParentHandlers(false);
        log.addHandler(captura);
    }

    @AfterEach
    void restaurarLog() {
        log.removeHandler(captura);
        log.setUseParentHandlers(usabaHandlersDelPadre);
    }

    private String salida() {
        return salida.toString(StandardCharsets.UTF_8);
    }

    private void assertSinDetallesTecnicos() {
        assertFalse(salida().contains("Exception"), salida());
        assertFalse(salida().contains("\tat "), salida());
    }

    @Test
    void errorDeDominioMuestraSuMensajeYNoSeRegistra() {
        manejador.ejecutar(() -> {
            throw new EntidadNoEncontradaException("un supervisor", "12345678A");
        });

        assertTrue(salida().contains("[ERROR] No existe un supervisor con identificador '12345678A'."));
        assertTrue(registros.isEmpty(), "un error de dominio puede llevar datos personales: no va al log");
    }

    @Test
    void accesoDatosMuestraMensajeGenericoSinElDetalleDeLaBd() {
        manejador.ejecutar(() -> {
            throw new AccesoDatosException("No se pudo completar la operación en la base de datos.",
                    new SQLException("Duplicate entry '12345678A' for key 'uq_supervisor_nif'"));
        });

        assertTrue(salida().contains(ManejadorErrores.MENSAJE_ACCESO_DATOS));
        assertFalse(salida().contains("Duplicate"));
        assertSinDetallesTecnicos();
        assertEquals(1, registros.size());
        assertFalse(registros.get(0).getMessage().contains("12345678A"));
        assertNull(registros.get(0).getThrown(), "al log no se adjunta la excepción ni su causa");
    }

    @Test
    void configuracionMuestraMensajeGenerico() {
        manejador.ejecutar(() -> {
            throw new ConfiguracionException("Falta la URL de la base de datos. Configura la variable DB_URL.");
        });

        assertTrue(salida().contains(ManejadorErrores.MENSAJE_CONFIGURACION));
        assertFalse(salida().contains("DB_URL"));
        assertEquals(1, registros.size());
    }

    @Test
    void excepcionInesperadaMuestraMensajeGenericoYRegistraTipoYMensaje() {
        manejador.ejecutar(() -> {
            throw new IllegalStateException("detalle interno");
        });

        assertTrue(salida().contains(ManejadorErrores.MENSAJE_INESPERADO));
        assertFalse(salida().contains("detalle interno"));
        assertSinDetallesTecnicos();
        assertEquals(1, registros.size());
        assertEquals("IllegalStateException: detalle interno", registros.get(0).getMessage());
        assertNull(registros.get(0).getThrown());
    }

    @Test
    void operacionCanceladaInformaSinErrorNiLog() {
        manejador.ejecutar(() -> {
            throw new OperacionCancelada();
        });

        assertTrue(salida().contains("Operación cancelada."));
        assertFalse(salida().contains("[ERROR]"));
        assertTrue(registros.isEmpty());
    }

    @Test
    void finDeEntradaSeDejaPasarParaTerminarLaAplicacion() {
        assertThrows(FinDeEntrada.class, () -> manejador.ejecutar(() -> {
            throw new FinDeEntrada();
        }));
    }

    @Test
    void sinErroresNoMuestraNada() {
        manejador.ejecutar(() -> { });

        assertEquals("", salida());
    }
}
