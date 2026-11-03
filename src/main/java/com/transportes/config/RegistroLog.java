package com.transportes.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Configura java.util.logging al arrancar: un único archivo logs/transportes.log, sin rotación
 * y sin ConsoleHandler, para que ningún error llegue a la consola con stack trace.
 */
public final class RegistroLog {

    static final Path ARCHIVO = Path.of("logs", "transportes.log");

    private RegistroLog() {
    }

    /** Devuelve false si no se pudo abrir el archivo; la aplicación sigue, pero sin log. */
    public static boolean configurar() {
        return configurar(ARCHIVO);
    }

    static boolean configurar(Path archivo) {
        Logger raiz = Logger.getLogger("");
        for (Handler handler : raiz.getHandlers()) {
            raiz.removeHandler(handler);
            handler.close();
        }
        raiz.setLevel(Level.INFO);
        try {
            Path carpeta = archivo.toAbsolutePath().getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }
            FileHandler handler = new FileHandler(archivo.toString(), true);
            handler.setEncoding(StandardCharsets.UTF_8.name());
            handler.setFormatter(new FormatoLinea());
            raiz.addHandler(handler);
            return true;
        } catch (IOException | SecurityException e) {
            return false;
        }
    }

    /**
     * Una línea por registro. De la excepción adjunta solo se escribe el tipo: su mensaje y su traza
     * pueden contener datos personales (p. ej. el NIF de un "Duplicate entry" de MySQL).
     */
    static final class FormatoLinea extends Formatter {

        @Override
        public String format(LogRecord registro) {
            ZonedDateTime momento = ZonedDateTime.ofInstant(registro.getInstant(), ZoneId.systemDefault());
            Throwable adjunta = registro.getThrown();
            return String.format("%1$tF %1$tT %2$s %3$s - %4$s%5$s%n",
                    momento, registro.getLevel().getName(), registro.getLoggerName(), formatMessage(registro),
                    adjunta == null ? "" : " [" + adjunta.getClass().getSimpleName() + "]");
        }
    }
}
