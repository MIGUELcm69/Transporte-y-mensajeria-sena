package com.transportes.controller;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;

/** Simula commit y rollback restaurando la instantánea de los DAO en memoria. */
final class TransaccionesEnMemoria implements Transacciones {

    /** Los DAO en memoria solo comprueban que reciben una conexión; nunca la usan. */
    static final Connection CONEXION_FICTICIA = (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
            (proxy, metodo, argumentos) -> {
                throw new UnsupportedOperationException("Conexión ficticia: " + metodo.getName());
            });

    private final List<ConInstantanea> daos;
    int confirmadas;
    int revertidas;

    TransaccionesEnMemoria(ConInstantanea... daos) {
        this.daos = List.of(daos);
    }

    @Override
    public <T> T ejecutar(TrabajoTransaccional<T> trabajo) {
        List<Runnable> restauraciones = daos.stream().map(ConInstantanea::instantanea).toList();
        try {
            T resultado = trabajo.ejecutar(CONEXION_FICTICIA);
            confirmadas++;
            return resultado;
        } catch (RuntimeException e) {
            restauraciones.forEach(Runnable::run);
            revertidas++;
            throw e;
        }
    }
}
