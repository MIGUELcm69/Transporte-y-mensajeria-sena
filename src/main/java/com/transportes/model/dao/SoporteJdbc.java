package com.transportes.model.dao;

import com.transportes.config.ConexionBD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Plantillas JDBC compartidas por los DAO: abren o reciben la conexión, preparan la sentencia
 * con parámetros y traducen cualquier SQLException.
 */
final class SoporteJdbc {

    @FunctionalInterface
    interface Operacion<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    @FunctionalInterface
    interface Parametros {
        void asignar(PreparedStatement sentencia) throws SQLException;
    }

    @FunctionalInterface
    interface Mapeador<T> {
        T mapear(ResultSet fila) throws SQLException;
    }

    static final Parametros SIN_PARAMETROS = sentencia -> { };

    private SoporteJdbc() {
    }

    /** Abre una conexión propia en auto-commit y la cierra al terminar. */
    static <T> T conConexionPropia(String operacion, Operacion<T> accion) {
        try (Connection conexion = ConexionBD.getConnection()) {
            return accion.ejecutar(conexion);
        } catch (SQLException e) {
            throw TraductorErroresSql.traducir(e, operacion);
        }
    }

    /** Usa la conexión del llamador: no la cierra ni hace commit ni rollback. */
    static <T> T conConexionExterna(Connection conexion, String operacion, Operacion<T> accion) {
        Objects.requireNonNull(conexion, "conexion");
        try {
            return accion.ejecutar(conexion);
        } catch (SQLException e) {
            throw TraductorErroresSql.traducir(e, operacion);
        }
    }

    static int insertar(Connection conexion, String sql, Parametros parametros) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            parametros.asignar(sentencia);
            sentencia.executeUpdate();
            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("La BD no devolvió el id generado.");
                }
                return claves.getInt(1);
            }
        }
    }

    /** Ejecuta INSERT/UPDATE/DELETE sin claves generadas y devuelve las filas afectadas. */
    static int modificar(Connection conexion, String sql, Parametros parametros) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            parametros.asignar(sentencia);
            return sentencia.executeUpdate();
        }
    }

    static <T> Optional<T> buscarUno(Connection conexion, String sql, Parametros parametros, Mapeador<T> mapeador)
            throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            parametros.asignar(sentencia);
            try (ResultSet filas = sentencia.executeQuery()) {
                return filas.next() ? Optional.of(mapeador.mapear(filas)) : Optional.empty();
            }
        }
    }

    static <T> List<T> buscarVarios(Connection conexion, String sql, Parametros parametros, Mapeador<T> mapeador)
            throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            parametros.asignar(sentencia);
            try (ResultSet filas = sentencia.executeQuery()) {
                List<T> resultado = new ArrayList<>();
                while (filas.next()) {
                    resultado.add(mapeador.mapear(filas));
                }
                return resultado;
            }
        }
    }

    /** Para consultas de una sola columna entera: COUNT(*) o EXISTS(...). */
    static int consultarEntero(Connection conexion, String sql, Parametros parametros) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            parametros.asignar(sentencia);
            try (ResultSet filas = sentencia.executeQuery()) {
                filas.next();
                return filas.getInt(1);
            }
        }
    }

    static void asignarEnteroOpcional(PreparedStatement sentencia, int indice, Integer valor) throws SQLException {
        if (valor == null) {
            sentencia.setNull(indice, Types.INTEGER);
        } else {
            sentencia.setInt(indice, valor);
        }
    }

    static void asignarDecimalOpcional(PreparedStatement sentencia, int indice, BigDecimal valor) throws SQLException {
        if (valor == null) {
            sentencia.setNull(indice, Types.DECIMAL);
        } else {
            sentencia.setBigDecimal(indice, valor);
        }
    }

    static void asignarTextoOpcional(PreparedStatement sentencia, int indice, String valor) throws SQLException {
        if (valor == null) {
            sentencia.setNull(indice, Types.VARCHAR);
        } else {
            sentencia.setString(indice, valor);
        }
    }

    static void asignarFecha(PreparedStatement sentencia, int indice, LocalDate fecha) throws SQLException {
        sentencia.setObject(indice, fecha);
    }

    static LocalDate leerFecha(ResultSet fila, String columna) throws SQLException {
        return fila.getObject(columna, LocalDate.class);
    }

    static Integer leerEnteroOpcional(ResultSet fila, String columna) throws SQLException {
        return fila.getObject(columna, Integer.class);
    }

    static int idPersistido(Integer id, String entidad) {
        if (id == null) {
            throw new IllegalArgumentException("No se puede actualizar " + entidad + " sin id.");
        }
        return id;
    }

    static void exigirSinId(Integer id, String entidad) {
        if (id != null) {
            throw new IllegalArgumentException("No se puede guardar " + entidad + " que ya tiene id " + id + ".");
        }
    }
}
