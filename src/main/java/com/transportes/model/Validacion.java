package com.transportes.model;

import com.transportes.exception.DatoInvalidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Comprobaciones de invariantes compartidas por las entidades del modelo.
 * Las longitudes máximas reflejan las columnas de sql/schema.sql.
 */
final class Validacion {

    static final int LONGITUD_CODIGO = 20;
    static final int LONGITUD_DESCRIPCION = 200;
    static final int LONGITUD_NOMBRE = 100;
    static final int LONGITUD_DIRECCION = 200;
    static final int LONGITUD_TELEFONO = 15;
    static final int LONGITUD_DOCUMENTO = 9;

    private Validacion() {
    }

    static String texto(String valor, String campo, int longitudMaxima) {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalidoException("El campo '" + campo + "' es obligatorio.");
        }
        String limpio = valor.trim();
        if (limpio.length() > longitudMaxima) {
            throw new DatoInvalidoException(
                    "El campo '" + campo + "' admite como máximo " + longitudMaxima + " caracteres.");
        }
        return limpio;
    }

    static <T> T obligatorio(T valor, String campo) {
        if (valor == null) {
            throw new DatoInvalidoException("El campo '" + campo + "' es obligatorio.");
        }
        return valor;
    }

    /** Importe monetario no negativo, normalizado a escala 2 con HALF_UP. */
    static BigDecimal importe(BigDecimal valor, String campo) {
        obligatorio(valor, campo);
        if (valor.signum() < 0) {
            throw new DatoInvalidoException("El campo '" + campo + "' no puede ser negativo.");
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal positivo(BigDecimal valor, String campo) {
        obligatorio(valor, campo);
        if (valor.signum() <= 0) {
            throw new DatoInvalidoException("El campo '" + campo + "' debe ser mayor que cero.");
        }
        return valor;
    }

    static int id(int valor, String campo) {
        if (valor <= 0) {
            throw new DatoInvalidoException("El campo '" + campo + "' debe ser un identificador válido.");
        }
        return valor;
    }

    static Integer idOpcional(Integer valor, String campo) {
        return valor == null ? null : id(valor, campo);
    }

    static int noNegativo(int valor, String campo) {
        if (valor < 0) {
            throw new DatoInvalidoException("El campo '" + campo + "' no puede ser negativo.");
        }
        return valor;
    }

    /** El id solo se asigna una vez, tras insertar la entidad en la BD. */
    static int idNuevo(Integer actual, int nuevo) {
        if (actual != null) {
            throw new IllegalStateException("La entidad ya tiene id asignado: " + actual);
        }
        return id(nuevo, "id");
    }
}
