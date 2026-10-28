package com.transportes.controller;

/**
 * Ejecuta un trabajo en una única transacción: commit si termina bien, rollback si lanza
 * cualquier RuntimeException, que se propaga sin cambios.
 */
public interface Transacciones {

    <T> T ejecutar(TrabajoTransaccional<T> trabajo);
}
