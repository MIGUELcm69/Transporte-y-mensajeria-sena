package com.transportes.controller;

import java.sql.Connection;

/**
 * Operaciones que se ejecutan dentro de una transacción. No declara SQLException porque los DAO
 * ya la traducen a excepciones no comprobadas.
 */
@FunctionalInterface
public interface TrabajoTransaccional<T> {

    T ejecutar(Connection conexion);
}
