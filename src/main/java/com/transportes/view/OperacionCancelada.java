package com.transportes.view;

/**
 * El usuario escribió la palabra de cancelar en un campo. Solo la lanza Consola por petición del usuario,
 * nunca por una entrada inválida; no es un error de dominio.
 */
public class OperacionCancelada extends RuntimeException {

    public OperacionCancelada() {
        super("Operación cancelada.");
    }
}
