package com.transportes.model.dao;

import com.transportes.model.Cliente;
import com.transportes.model.ClienteEmpresa;
import com.transportes.model.ClienteParticular;
import com.transportes.model.TipoCliente;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ClienteDaoImpl implements ClienteDao {

    private static final String SELECT =
            "SELECT id, codigo_cliente, fecha_alta, total_facturado, tipo, nif, cif, nombre, direccion, telefono"
                    + " FROM cliente";
    private static final String SELECT_POR_ID = SELECT + " WHERE id = ?";
    private static final String SELECT_POR_CODIGO = SELECT + " WHERE codigo_cliente = ?";
    private static final String SELECT_TODOS = SELECT + " ORDER BY id";
    private static final String INSERT =
            "INSERT INTO cliente (codigo_cliente, fecha_alta, total_facturado, tipo, nif, cif, nombre, direccion,"
                    + " telefono) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE =
            "UPDATE cliente SET nif = ?, cif = ?, nombre = ?, direccion = ?, telefono = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM cliente WHERE id = ?";
    private static final String SUMAR_TOTAL_FACTURADO =
            "UPDATE cliente SET total_facturado = total_facturado + ? WHERE id = ?";

    @Override
    public void save(Cliente cliente) {
        SoporteJdbc.conConexionPropia("guardar cliente", c -> insertar(cliente, c));
    }

    @Override
    public void save(Cliente cliente, Connection conexion) {
        SoporteJdbc.conConexionExterna(conexion, "guardar cliente", c -> insertar(cliente, c));
    }

    @Override
    public boolean update(Cliente cliente) {
        return SoporteJdbc.conConexionPropia("actualizar cliente", c -> actualizar(cliente, c));
    }

    @Override
    public boolean update(Cliente cliente, Connection conexion) {
        return SoporteJdbc.conConexionExterna(conexion, "actualizar cliente", c -> actualizar(cliente, c));
    }

    @Override
    public boolean delete(int id) {
        return SoporteJdbc.conConexionPropia("eliminar cliente", c -> eliminar(id, c));
    }

    @Override
    public boolean delete(int id, Connection conexion) {
        return SoporteJdbc.conConexionExterna(conexion, "eliminar cliente", c -> eliminar(id, c));
    }

    @Override
    public Optional<Cliente> findById(int id) {
        return SoporteJdbc.conConexionPropia("buscar cliente por id",
                c -> SoporteJdbc.buscarUno(c, SELECT_POR_ID, s -> s.setInt(1, id), ClienteDaoImpl::mapear));
    }

    @Override
    public Optional<Cliente> findByCodigo(String codigoCliente) {
        return SoporteJdbc.conConexionPropia("buscar cliente por código",
                c -> SoporteJdbc.buscarUno(c, SELECT_POR_CODIGO, s -> s.setString(1, codigoCliente),
                        ClienteDaoImpl::mapear));
    }

    @Override
    public List<Cliente> findAll() {
        return SoporteJdbc.conConexionPropia("listar clientes",
                c -> SoporteJdbc.buscarVarios(c, SELECT_TODOS, SoporteJdbc.SIN_PARAMETROS, ClienteDaoImpl::mapear));
    }

    @Override
    public boolean sumarTotalFacturado(int clienteId, BigDecimal delta) {
        return SoporteJdbc.conConexionPropia("actualizar total facturado",
                c -> sumar(clienteId, delta, c));
    }

    @Override
    public boolean sumarTotalFacturado(int clienteId, BigDecimal delta, Connection conexion) {
        return SoporteJdbc.conConexionExterna(conexion, "actualizar total facturado",
                c -> sumar(clienteId, delta, c));
    }

    private static Void insertar(Cliente cliente, Connection conexion) throws SQLException {
        SoporteJdbc.exigirSinId(cliente.getId(), "un cliente");
        int id = SoporteJdbc.insertar(conexion, INSERT, s -> {
            s.setString(1, cliente.getCodigoCliente());
            SoporteJdbc.asignarFecha(s, 2, cliente.getFechaAlta());
            s.setBigDecimal(3, cliente.getTotalFacturado());
            s.setString(4, cliente.getTipo().name());
            asignarDatos(s, 5, cliente);
        });
        cliente.asignarId(id);
        return null;
    }

    private static boolean actualizar(Cliente cliente, Connection conexion) throws SQLException {
        int id = SoporteJdbc.idPersistido(cliente.getId(), "un cliente");
        return SoporteJdbc.modificar(conexion, UPDATE, s -> {
            asignarDatos(s, 1, cliente);
            s.setInt(6, id);
        }) > 0;
    }

    private static boolean eliminar(int id, Connection conexion) throws SQLException {
        return SoporteJdbc.modificar(conexion, DELETE, s -> s.setInt(1, id)) > 0;
    }

    private static boolean sumar(int clienteId, BigDecimal delta, Connection conexion) throws SQLException {
        Objects.requireNonNull(delta, "delta");
        return SoporteJdbc.modificar(conexion, SUMAR_TOTAL_FACTURADO, s -> {
            s.setBigDecimal(1, delta);
            s.setInt(2, clienteId);
        }) > 0;
    }

    /** Asigna, desde {@code inicio}, nif, cif, nombre, direccion y telefono. */
    private static void asignarDatos(PreparedStatement sentencia, int inicio, Cliente cliente) throws SQLException {
        ClienteParticular particular = cliente instanceof ClienteParticular p ? p : null;
        ClienteEmpresa empresa = cliente instanceof ClienteEmpresa e ? e : null;
        SoporteJdbc.asignarTextoOpcional(sentencia, inicio, particular == null ? null : particular.getNif());
        SoporteJdbc.asignarTextoOpcional(sentencia, inicio + 1, empresa == null ? null : empresa.getCif());
        sentencia.setString(inicio + 2, cliente.getNombre());
        sentencia.setString(inicio + 3, cliente.getDireccion());
        SoporteJdbc.asignarTextoOpcional(sentencia, inicio + 4, empresa == null ? null : empresa.getTelefono());
    }

    private static Cliente mapear(ResultSet fila) throws SQLException {
        int id = fila.getInt("id");
        String codigoCliente = fila.getString("codigo_cliente");
        LocalDate fechaAlta = SoporteJdbc.leerFecha(fila, "fecha_alta");
        BigDecimal totalFacturado = fila.getBigDecimal("total_facturado");
        String nombre = fila.getString("nombre");
        String direccion = fila.getString("direccion");
        return switch (TipoCliente.valueOf(fila.getString("tipo"))) {
            case PARTICULAR -> new ClienteParticular(id, codigoCliente, fechaAlta, totalFacturado,
                    fila.getString("nif"), nombre, direccion);
            case EMPRESA -> new ClienteEmpresa(id, codigoCliente, fechaAlta, totalFacturado,
                    fila.getString("cif"), nombre, direccion, fila.getString("telefono"));
        };
    }
}
