package com.transportes.model.dao;

import com.transportes.model.Supervisor;
import com.transportes.model.TipoVehiculo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class SupervisorDaoImpl implements SupervisorDao {

    private static final String SELECT =
            "SELECT id, nif, nombre, direccion, telefono FROM supervisor";
    private static final String SELECT_POR_ID = SELECT + " WHERE id = ?";
    private static final String SELECT_POR_NIF = SELECT + " WHERE nif = ?";
    private static final String SELECT_TODOS = SELECT + " ORDER BY id";
    private static final String INSERT =
            "INSERT INTO supervisor (nif, nombre, direccion, telefono) VALUES (?, ?, ?, ?)";
    private static final String UPDATE =
            "UPDATE supervisor SET nif = ?, nombre = ?, direccion = ?, telefono = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM supervisor WHERE id = ?";
    private static final String CONTAR_AVIONES =
            "SELECT COUNT(*) FROM vehiculo WHERE supervisor_id = ? AND tipo = ?";

    @Override
    public void save(Supervisor supervisor) {
        SoporteJdbc.conConexionPropia("guardar supervisor", c -> insertar(supervisor, c));
    }

    @Override
    public void save(Supervisor supervisor, Connection conexion) {
        SoporteJdbc.conConexionExterna(conexion, "guardar supervisor", c -> insertar(supervisor, c));
    }

    @Override
    public boolean update(Supervisor supervisor) {
        return SoporteJdbc.conConexionPropia("actualizar supervisor", c -> actualizar(supervisor, c));
    }

    @Override
    public boolean update(Supervisor supervisor, Connection conexion) {
        return SoporteJdbc.conConexionExterna(conexion, "actualizar supervisor", c -> actualizar(supervisor, c));
    }

    @Override
    public boolean delete(int id) {
        return SoporteJdbc.conConexionPropia("eliminar supervisor", c -> eliminar(id, c));
    }

    @Override
    public boolean delete(int id, Connection conexion) {
        return SoporteJdbc.conConexionExterna(conexion, "eliminar supervisor", c -> eliminar(id, c));
    }

    @Override
    public Optional<Supervisor> findById(int id) {
        return SoporteJdbc.conConexionPropia("buscar supervisor por id",
                c -> SoporteJdbc.buscarUno(c, SELECT_POR_ID, s -> s.setInt(1, id), SupervisorDaoImpl::mapear));
    }

    @Override
    public Optional<Supervisor> findByNif(String nif) {
        return SoporteJdbc.conConexionPropia("buscar supervisor por NIF",
                c -> SoporteJdbc.buscarUno(c, SELECT_POR_NIF, s -> s.setString(1, nif), SupervisorDaoImpl::mapear));
    }

    @Override
    public List<Supervisor> findAll() {
        return SoporteJdbc.conConexionPropia("listar supervisores",
                c -> SoporteJdbc.buscarVarios(c, SELECT_TODOS, SoporteJdbc.SIN_PARAMETROS, SupervisorDaoImpl::mapear));
    }

    @Override
    public int contarAviones(int supervisorId) {
        return SoporteJdbc.conConexionPropia("contar aviones del supervisor",
                c -> SoporteJdbc.consultarEntero(c, CONTAR_AVIONES, s -> {
                    s.setInt(1, supervisorId);
                    s.setString(2, TipoVehiculo.AVION.name());
                }));
    }

    private static Void insertar(Supervisor supervisor, Connection conexion) throws SQLException {
        SoporteJdbc.exigirSinId(supervisor.getId(), "un supervisor");
        int id = SoporteJdbc.insertar(conexion, INSERT, s -> asignarDatos(s, supervisor));
        supervisor.asignarId(id);
        return null;
    }

    private static boolean actualizar(Supervisor supervisor, Connection conexion) throws SQLException {
        int id = SoporteJdbc.idPersistido(supervisor.getId(), "un supervisor");
        return SoporteJdbc.modificar(conexion, UPDATE, s -> {
            asignarDatos(s, supervisor);
            s.setInt(5, id);
        }) > 0;
    }

    private static boolean eliminar(int id, Connection conexion) throws SQLException {
        return SoporteJdbc.modificar(conexion, DELETE, s -> s.setInt(1, id)) > 0;
    }

    private static void asignarDatos(PreparedStatement sentencia, Supervisor supervisor) throws SQLException {
        sentencia.setString(1, supervisor.getNif());
        sentencia.setString(2, supervisor.getNombre());
        sentencia.setString(3, supervisor.getDireccion());
        sentencia.setString(4, supervisor.getTelefono());
    }

    private static Supervisor mapear(ResultSet fila) throws SQLException {
        return new Supervisor(
                fila.getInt("id"),
                fila.getString("nif"),
                fila.getString("nombre"),
                fila.getString("direccion"),
                fila.getString("telefono"));
    }
}
