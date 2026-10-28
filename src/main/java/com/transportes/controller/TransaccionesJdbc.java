package com.transportes.controller;

import com.transportes.config.ConexionBD;
import com.transportes.exception.AccesoDatosException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransaccionesJdbc implements Transacciones {

    private static final Logger LOG = Logger.getLogger(TransaccionesJdbc.class.getName());

    @Override
    public <T> T ejecutar(TrabajoTransaccional<T> trabajo) {
        try (Connection conexion = ConexionBD.getConnection()) {
            conexion.setAutoCommit(false);
            try {
                T resultado = trabajo.ejecutar(conexion);
                conexion.commit();
                return resultado;
            } catch (RuntimeException | SQLException e) {
                revertir(conexion, e);
                throw e;
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error al abrir, confirmar o cerrar la transacción", e);
            throw new AccesoDatosException("No se pudo completar la operación en la base de datos.", e);
        }
    }

    private static void revertir(Connection conexion, Exception causa) {
        try {
            conexion.rollback();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Error al hacer rollback", e);
            causa.addSuppressed(e);
        }
    }
}
