package com.transportes.model.dao;

import com.transportes.model.Envio;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class EnvioDaoImpl implements EnvioDao {

    private static final String SELECT =
            "SELECT id, cliente_origen_id, destinatario_id, mercancia_id, fecha_envio, intentos_fallidos, precio_total"
                    + " FROM envio";
    private static final String SELECT_POR_ID = SELECT + " WHERE id = ?";
    private static final String SELECT_POR_CLIENTE = SELECT + " WHERE cliente_origen_id = ? ORDER BY fecha_envio, id";
    private static final String SELECT_TODOS = SELECT + " ORDER BY id";
    private static final String INSERT =
            "INSERT INTO envio (cliente_origen_id, destinatario_id, mercancia_id, fecha_envio, intentos_fallidos,"
                    + " precio_total) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE =
            "UPDATE envio SET cliente_origen_id = ?, destinatario_id = ?, mercancia_id = ?, fecha_envio = ?,"
                    + " intentos_fallidos = ?, precio_total = ? WHERE id = ?";
    private static final String ACTUALIZAR_INTENTOS =
            "UPDATE envio SET intentos_fallidos = ?, precio_total = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM envio WHERE id = ?";

    @Override
    public void save(Envio envio) {
        SoporteJdbc.conConexionPropia("guardar envío", c -> insertar(envio, c));
    }

    @Override
    public void save(Envio envio, Connection conexion) {
        SoporteJdbc.conConexionExterna(conexion, "guardar envío", c -> insertar(envio, c));
    }

    @Override
    public boolean update(Envio envio) {
        return SoporteJdbc.conConexionPropia("actualizar envío", c -> actualizar(envio, c));
    }

    @Override
    public boolean update(Envio envio, Connection conexion) {
        return SoporteJdbc.conConexionExterna(conexion, "actualizar envío", c -> actualizar(envio, c));
    }

    @Override
    public boolean delete(int id) {
        return SoporteJdbc.conConexionPropia("eliminar envío", c -> eliminar(id, c));
    }

    @Override
    public boolean delete(int id, Connection conexion) {
        return SoporteJdbc.conConexionExterna(conexion, "eliminar envío", c -> eliminar(id, c));
    }

    @Override
    public Optional<Envio> findById(int id) {
        return SoporteJdbc.conConexionPropia("buscar envío por id",
                c -> SoporteJdbc.buscarUno(c, SELECT_POR_ID, s -> s.setInt(1, id), EnvioDaoImpl::mapear));
    }

    @Override
    public List<Envio> findByCliente(int clienteOrigenId) {
        return SoporteJdbc.conConexionPropia("listar envíos del cliente",
                c -> SoporteJdbc.buscarVarios(c, SELECT_POR_CLIENTE, s -> s.setInt(1, clienteOrigenId),
                        EnvioDaoImpl::mapear));
    }

    @Override
    public List<Envio> findAll() {
        return SoporteJdbc.conConexionPropia("listar envíos",
                c -> SoporteJdbc.buscarVarios(c, SELECT_TODOS, SoporteJdbc.SIN_PARAMETROS, EnvioDaoImpl::mapear));
    }

    @Override
    public boolean actualizarIntentos(Envio envio) {
        return SoporteJdbc.conConexionPropia("registrar intento fallido", c -> guardarIntentos(envio, c));
    }

    @Override
    public boolean actualizarIntentos(Envio envio, Connection conexion) {
        return SoporteJdbc.conConexionExterna(conexion, "registrar intento fallido", c -> guardarIntentos(envio, c));
    }

    private static Void insertar(Envio envio, Connection conexion) throws SQLException {
        SoporteJdbc.exigirSinId(envio.getId(), "un envío");
        int id = SoporteJdbc.insertar(conexion, INSERT, s -> asignarDatos(s, envio));
        envio.asignarId(id);
        return null;
    }

    private static boolean actualizar(Envio envio, Connection conexion) throws SQLException {
        int id = SoporteJdbc.idPersistido(envio.getId(), "un envío");
        return SoporteJdbc.modificar(conexion, UPDATE, s -> {
            asignarDatos(s, envio);
            s.setInt(7, id);
        }) > 0;
    }

    private static boolean guardarIntentos(Envio envio, Connection conexion) throws SQLException {
        int id = SoporteJdbc.idPersistido(envio.getId(), "un envío");
        return SoporteJdbc.modificar(conexion, ACTUALIZAR_INTENTOS, s -> {
            s.setInt(1, envio.getIntentosFallidos());
            s.setBigDecimal(2, envio.getPrecioTotal());
            s.setInt(3, id);
        }) > 0;
    }

    private static boolean eliminar(int id, Connection conexion) throws SQLException {
        return SoporteJdbc.modificar(conexion, DELETE, s -> s.setInt(1, id)) > 0;
    }

    private static void asignarDatos(PreparedStatement sentencia, Envio envio) throws SQLException {
        sentencia.setInt(1, envio.getClienteOrigenId());
        sentencia.setInt(2, envio.getDestinatarioId());
        sentencia.setInt(3, envio.getMercanciaId());
        SoporteJdbc.asignarFecha(sentencia, 4, envio.getFechaEnvio());
        sentencia.setInt(5, envio.getIntentosFallidos());
        sentencia.setBigDecimal(6, envio.getPrecioTotal());
    }

    private static Envio mapear(ResultSet fila) throws SQLException {
        return new Envio(
                fila.getInt("id"),
                fila.getInt("cliente_origen_id"),
                fila.getInt("destinatario_id"),
                fila.getInt("mercancia_id"),
                SoporteJdbc.leerFecha(fila, "fecha_envio"),
                fila.getInt("intentos_fallidos"),
                fila.getBigDecimal("precio_total"));
    }
}
