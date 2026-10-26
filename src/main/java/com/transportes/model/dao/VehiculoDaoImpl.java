package com.transportes.model.dao;

import com.transportes.model.Avion;
import com.transportes.model.Furgoneta;
import com.transportes.model.Moto;
import com.transportes.model.TipoVehiculo;
import com.transportes.model.Vehiculo;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class VehiculoDaoImpl implements VehiculoDao {

    private static final String SELECT =
            "SELECT id, codigo, tipo, descripcion, fecha_compra, precio_compra, supervisor_id, cilindrada, capacidad"
                    + " FROM vehiculo";
    private static final String SELECT_POR_ID = SELECT + " WHERE id = ?";
    private static final String SELECT_POR_CODIGO = SELECT + " WHERE codigo = ?";
    private static final String SELECT_POR_TIPO = SELECT + " WHERE tipo = ? ORDER BY id";
    private static final String SELECT_POR_SUPERVISOR = SELECT + " WHERE supervisor_id = ? ORDER BY id";
    private static final String SELECT_TODOS = SELECT + " ORDER BY id";
    private static final String INSERT =
            "INSERT INTO vehiculo (codigo, tipo, descripcion, fecha_compra, precio_compra, supervisor_id,"
                    + " cilindrada, capacidad) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE =
            "UPDATE vehiculo SET descripcion = ?, fecha_compra = ?, precio_compra = ?, supervisor_id = ?,"
                    + " cilindrada = ?, capacidad = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM vehiculo WHERE id = ?";
    private static final String CONTAR_POR_SUPERVISOR = "SELECT COUNT(*) FROM vehiculo WHERE supervisor_id = ?";
    private static final String TIENE_MERCANCIAS =
            "SELECT EXISTS (SELECT 1 FROM mercancia"
                    + " WHERE vehiculo_id = ? OR avion_id = ? OR vehiculo_destino_id = ?)";

    @Override
    public void save(Vehiculo vehiculo) {
        SoporteJdbc.conConexionPropia("guardar vehículo", c -> insertar(vehiculo, c));
    }

    @Override
    public void save(Vehiculo vehiculo, Connection conexion) {
        SoporteJdbc.conConexionExterna(conexion, "guardar vehículo", c -> insertar(vehiculo, c));
    }

    @Override
    public boolean update(Vehiculo vehiculo) {
        return SoporteJdbc.conConexionPropia("actualizar vehículo", c -> actualizar(vehiculo, c));
    }

    @Override
    public boolean update(Vehiculo vehiculo, Connection conexion) {
        return SoporteJdbc.conConexionExterna(conexion, "actualizar vehículo", c -> actualizar(vehiculo, c));
    }

    @Override
    public boolean delete(int id) {
        return SoporteJdbc.conConexionPropia("eliminar vehículo", c -> eliminar(id, c));
    }

    @Override
    public boolean delete(int id, Connection conexion) {
        return SoporteJdbc.conConexionExterna(conexion, "eliminar vehículo", c -> eliminar(id, c));
    }

    @Override
    public Optional<Vehiculo> findById(int id) {
        return SoporteJdbc.conConexionPropia("buscar vehículo por id",
                c -> SoporteJdbc.buscarUno(c, SELECT_POR_ID, s -> s.setInt(1, id), VehiculoDaoImpl::mapear));
    }

    @Override
    public Optional<Vehiculo> findByCodigo(String codigo) {
        return SoporteJdbc.conConexionPropia("buscar vehículo por código",
                c -> SoporteJdbc.buscarUno(c, SELECT_POR_CODIGO, s -> s.setString(1, codigo), VehiculoDaoImpl::mapear));
    }

    @Override
    public List<Vehiculo> findByTipo(TipoVehiculo tipo) {
        return SoporteJdbc.conConexionPropia("listar vehículos por tipo",
                c -> SoporteJdbc.buscarVarios(c, SELECT_POR_TIPO, s -> s.setString(1, tipo.name()),
                        VehiculoDaoImpl::mapear));
    }

    @Override
    public List<Vehiculo> findBySupervisor(int supervisorId) {
        return SoporteJdbc.conConexionPropia("listar vehículos del supervisor",
                c -> SoporteJdbc.buscarVarios(c, SELECT_POR_SUPERVISOR, s -> s.setInt(1, supervisorId),
                        VehiculoDaoImpl::mapear));
    }

    @Override
    public List<Vehiculo> findAll() {
        return SoporteJdbc.conConexionPropia("listar vehículos",
                c -> SoporteJdbc.buscarVarios(c, SELECT_TODOS, SoporteJdbc.SIN_PARAMETROS, VehiculoDaoImpl::mapear));
    }

    @Override
    public int contarPorSupervisor(int supervisorId) {
        return SoporteJdbc.conConexionPropia("contar vehículos del supervisor",
                c -> SoporteJdbc.consultarEntero(c, CONTAR_POR_SUPERVISOR, s -> s.setInt(1, supervisorId)));
    }

    @Override
    public boolean tieneMercancias(int vehiculoId) {
        return SoporteJdbc.conConexionPropia("comprobar mercancías del vehículo",
                c -> SoporteJdbc.consultarEntero(c, TIENE_MERCANCIAS, s -> {
                    s.setInt(1, vehiculoId);
                    s.setInt(2, vehiculoId);
                    s.setInt(3, vehiculoId);
                }) == 1);
    }

    private static Void insertar(Vehiculo vehiculo, Connection conexion) throws SQLException {
        SoporteJdbc.exigirSinId(vehiculo.getId(), "un vehículo");
        int id = SoporteJdbc.insertar(conexion, INSERT, s -> {
            s.setString(1, vehiculo.getCodigo());
            s.setString(2, vehiculo.getTipo().name());
            asignarDatos(s, 3, vehiculo);
        });
        vehiculo.asignarId(id);
        return null;
    }

    private static boolean actualizar(Vehiculo vehiculo, Connection conexion) throws SQLException {
        int id = SoporteJdbc.idPersistido(vehiculo.getId(), "un vehículo");
        return SoporteJdbc.modificar(conexion, UPDATE, s -> {
            asignarDatos(s, 1, vehiculo);
            s.setInt(7, id);
        }) > 0;
    }

    private static boolean eliminar(int id, Connection conexion) throws SQLException {
        return SoporteJdbc.modificar(conexion, DELETE, s -> s.setInt(1, id)) > 0;
    }

    /** Asigna, desde {@code inicio}, descripcion, fecha_compra, precio_compra, supervisor_id, cilindrada y capacidad. */
    private static void asignarDatos(PreparedStatement sentencia, int inicio, Vehiculo vehiculo) throws SQLException {
        sentencia.setString(inicio, vehiculo.getDescripcion());
        SoporteJdbc.asignarFecha(sentencia, inicio + 1, vehiculo.getFechaCompra());
        sentencia.setBigDecimal(inicio + 2, vehiculo.getPrecioCompra());
        sentencia.setInt(inicio + 3, vehiculo.getSupervisorId());
        SoporteJdbc.asignarDecimalOpcional(sentencia, inicio + 4,
                vehiculo instanceof Moto moto ? moto.getCilindrada() : null);
        SoporteJdbc.asignarDecimalOpcional(sentencia, inicio + 5,
                vehiculo instanceof Furgoneta furgoneta ? furgoneta.getCapacidad() : null);
    }

    private static Vehiculo mapear(ResultSet fila) throws SQLException {
        int id = fila.getInt("id");
        String codigo = fila.getString("codigo");
        String descripcion = fila.getString("descripcion");
        LocalDate fechaCompra = SoporteJdbc.leerFecha(fila, "fecha_compra");
        BigDecimal precioCompra = fila.getBigDecimal("precio_compra");
        int supervisorId = fila.getInt("supervisor_id");
        return switch (TipoVehiculo.valueOf(fila.getString("tipo"))) {
            case MOTO -> new Moto(id, codigo, descripcion, fechaCompra, precioCompra, supervisorId,
                    fila.getBigDecimal("cilindrada"));
            case FURGONETA -> new Furgoneta(id, codigo, descripcion, fechaCompra, precioCompra, supervisorId,
                    fila.getBigDecimal("capacidad"));
            case AVION -> new Avion(id, codigo, descripcion, fechaCompra, precioCompra, supervisorId);
        };
    }
}
