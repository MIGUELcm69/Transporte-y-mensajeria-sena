package com.transportes.view;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Lectura y validación de entradas. Ante una entrada inválida informa y vuelve a preguntar; solo sale con
 * {@link OperacionCancelada} si el usuario escribe {@value #CANCELAR} en un campo, o con {@link FinDeEntrada}
 * si la entrada se agota.
 */
public class Consola {

    /** Escrita en cualquier campo (no en los menús), abandona la operación en curso. */
    public static final String CANCELAR = "cancelar";

    public enum Rango {
        NO_NEGATIVO(true, "El valor no puede ser negativo."),
        POSITIVO(false, "El valor debe ser mayor que cero.");

        private final boolean admiteCero;
        private final String error;

        Rango(boolean admiteCero, String error) {
            this.admiteCero = admiteCero;
            this.error = error;
        }

        boolean admite(BigDecimal valor) {
            return valor.signum() > 0 || (admiteCero && valor.signum() == 0);
        }
    }

    private static final Pattern ENTERO = Pattern.compile("-?\\d+");
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+([.,]\\d+)?");

    @FunctionalInterface
    private interface Interprete<T> {
        T interpretar(String texto) throws EntradaInvalida;
    }

    private static final class EntradaInvalida extends Exception {
        EntradaInvalida(String mensaje) {
            super(mensaje);
        }
    }

    private final Scanner entrada;
    private final PrintStream salida;

    public Consola(Scanner entrada, PrintStream salida) {
        this.entrada = Objects.requireNonNull(entrada, "entrada");
        this.salida = Objects.requireNonNull(salida, "salida");
    }

    /** Muestra las opciones numeradas desde 1 y {@code textoCero} como 0. Devuelve el número elegido. */
    public int elegirOpcion(String titulo, List<String> opciones, String textoCero) {
        salida.println();
        salida.println("=== " + titulo + " ===");
        for (int i = 0; i < opciones.size(); i++) {
            salida.println("  " + (i + 1) + ". " + opciones.get(i));
        }
        salida.println("  0. " + textoCero);
        while (true) {
            String texto = leerLinea("Opción: ");
            try {
                return entero(texto, 0, opciones.size());
            } catch (EntradaInvalida e) {
                error("Opción no válida. " + e.getMessage());
            }
        }
    }

    public int leerEntero(String etiqueta, int min, int max) {
        return leer(etiqueta + ": ", texto -> entero(texto, min, max));
    }

    /** Acepta coma o punto como separador decimal, sin separador de miles. */
    public BigDecimal leerDecimal(String etiqueta, Rango rango) {
        return leer(etiqueta + ": ", texto -> decimal(texto, rango));
    }

    /** Enter no pone ningún valor por defecto: vuelve a preguntar. */
    public LocalDate leerFecha(String etiqueta) {
        return leer(etiqueta + " (dd/mm/aaaa): ", Consola::fecha);
    }

    /** Enter usa {@code hoy} y lo muestra. */
    public LocalDate leerFechaOHoy(String etiqueta, LocalDate hoy) {
        String textoHoy = Formato.fecha(hoy);
        return leer(etiqueta + " (dd/mm/aaaa, Enter = hoy " + textoHoy + "): ", texto -> {
            if (texto.isEmpty()) {
                mostrar("Se usa la fecha de hoy: " + textoHoy + ".");
                return hoy;
            }
            return fecha(texto);
        });
    }

    /** Devuelve el texto sin espacios al principio ni al final. */
    public String leerTextoNoVacio(String etiqueta) {
        return leer(etiqueta + ": ", Consola::noVacio);
    }

    public boolean confirmar(String pregunta) {
        return leer(pregunta + " (s/n): ", Consola::siNo);
    }

    // ----- Variantes para modificar: Enter conserva el valor actual -----

    public String leerTextoOActual(String etiqueta, String actual) {
        return leer(conActual(etiqueta, actual), texto -> texto.isEmpty() ? actual : texto);
    }

    public BigDecimal leerDecimalOActual(String etiqueta, Rango rango, BigDecimal actual) {
        return leer(conActual(etiqueta, actual.toPlainString()),
                texto -> texto.isEmpty() ? actual : decimal(texto, rango));
    }

    public LocalDate leerFechaOActual(String etiqueta, LocalDate actual) {
        return leer(conActual(etiqueta + " (dd/mm/aaaa)", Formato.fecha(actual)),
                texto -> texto.isEmpty() ? actual : fecha(texto));
    }

    public boolean confirmarOActual(String pregunta, boolean actual) {
        return leer(conActual(pregunta + " (s/n)", actual ? "s" : "n"),
                texto -> texto.isEmpty() ? actual : siNo(texto));
    }

    // ----- Salida -----

    public void mostrar(String mensaje) {
        salida.println(mensaje);
    }

    public void exito(String mensaje) {
        salida.println("[OK] " + mensaje);
    }

    public void error(String mensaje) {
        salida.println("[ERROR] " + mensaje);
    }

    // ----- Lectura -----

    private <T> T leer(String indicacion, Interprete<T> interprete) {
        while (true) {
            String texto = leerLinea(indicacion);
            if (texto.equalsIgnoreCase(CANCELAR)) {
                throw new OperacionCancelada();
            }
            try {
                return interprete.interpretar(texto);
            } catch (EntradaInvalida e) {
                error(e.getMessage());
            }
        }
    }

    private String leerLinea(String indicacion) {
        salida.print(indicacion);
        salida.flush();
        if (!entrada.hasNextLine()) {
            throw new FinDeEntrada();
        }
        return entrada.nextLine().strip();
    }

    private static String conActual(String etiqueta, String actual) {
        return etiqueta + " [" + actual + ", Enter = conservar]: ";
    }

    private static int entero(String texto, int min, int max) throws EntradaInvalida {
        String fueraDeRango = "Introduzca un número entero entre " + min + " y " + max + ".";
        if (!ENTERO.matcher(texto).matches()) {
            throw new EntradaInvalida(fueraDeRango);
        }
        int valor;
        try {
            valor = Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            throw new EntradaInvalida(fueraDeRango);
        }
        if (valor < min || valor > max) {
            throw new EntradaInvalida(fueraDeRango);
        }
        return valor;
    }

    private static BigDecimal decimal(String texto, Rango rango) throws EntradaInvalida {
        if (!DECIMAL.matcher(texto).matches()) {
            throw new EntradaInvalida("Introduzca un número, por ejemplo 12,50 o 12.50 (sin separador de miles).");
        }
        BigDecimal valor = new BigDecimal(texto.replace(',', '.'));
        if (!rango.admite(valor)) {
            throw new EntradaInvalida(rango.error);
        }
        return valor;
    }

    private static LocalDate fecha(String texto) throws EntradaInvalida {
        try {
            return LocalDate.parse(texto, Formato.FECHA);
        } catch (DateTimeParseException e) {
            throw new EntradaInvalida("Fecha no válida. Use el formato dd/mm/aaaa, por ejemplo 15/03/2026.");
        }
    }

    private static String noVacio(String texto) throws EntradaInvalida {
        if (texto.isEmpty()) {
            throw new EntradaInvalida("Este campo es obligatorio.");
        }
        return texto;
    }

    private static boolean siNo(String texto) throws EntradaInvalida {
        return switch (texto.toLowerCase(Locale.ROOT)) {
            case "s", "si", "sí" -> true;
            case "n", "no" -> false;
            default -> throw new EntradaInvalida("Responda s (sí) o n (no).");
        };
    }
}
