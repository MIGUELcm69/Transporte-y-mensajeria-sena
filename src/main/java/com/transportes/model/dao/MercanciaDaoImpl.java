package com.transportes.model.dao;

import com.transportes.model.Mercancia;
import com.transportes.model.Paquete;
import com.transportes.model.Sobre;
import com.transportes.model.TipoMercancia;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class MercanciaDaoImpl implements MercanciaDao {

    private static final String SELECT =
            "SELECT id, codigo, descripcion, precio_envio, asegurada, largo_recorrido, tipo, peso, largo, ancho, alto,"
                    + " vehiculo_id, avion_id, vehiculo_destino_id FROM mercancia";
    private static final String SELECT_POR_ID = SELECT + " WHERE id = ?";
    private static final String SELECT_POR_CODIGO = SELECT + " WHERE codigo = ?";
    private static final String SELECT_POR_VEHICULO =
            SELECT + " WHERE vehiculo_id = ? OR avion_id = ? OR vehiculo_destino_id = ? ORDER BY id";
    private static final String SELECT_LARGO_RECORRIDO = SELECT + " WHERE largo_recorrido = ? ORDER BY id";
    private static final String SELECT_TODOS = SELECT + " ORDER BY id";
    private static final String INSERT =
            "INSERT INTO mercancia (codigo, tipo, descripcion, precio_envio, asegurada, largo_recorrido, peso, largo,"
                    + " ancho, alto, vehiculo_id, avion_id, vehiculo_destino_id)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE =
            "UPDATE mercancia SET descripcion = ?, precio_envio = ?, asegurada = ?, largo_recorrido = ?, peso = ?,"
                    + " largo = ?, ancho = ?, alto = ?, vehiculo_id = ?, avion_id = ?, vehiculo_destino_id = ?"
                    + " WHERE id = ?";
    private static final String DELETE = "DELETE FROM mercancia WHERE id = ?";
    private static final String TIENE_ENVIO = "SELECT EXISTS (SELECT 1 FROM envio WHERE mercancia_id = ?)";

    @Override
    public void save(Mercancia mercancia) {
        SoporteJdbc.conConexionPropia("guardar mercancía", c -> insertar(mercancia, c));
    }

    @Override
    public void save(Mercancia mercancia, Connection conexion) {
        SoporteJdbc.conConexionExterna(conexion, "guardar mercancía", c -> insertar(mercancia, c));
    }

    @Override
    public boolean update(Mercancia mercancia) {
        return SoporteJdbc.conConexionPropia("actualizar mercancía", c -> actualizar(mercancia, c));
    }

    @Override
    public boolean update(Mercancia mercancia, Connection conexion) {
        return SoporteJdbc.conConexionExterna(conexion, "actualizar mercancía", c -> actualizar(mercancia, c));
    }

    @Override
    public boolean delete(int id) {
        return SoporteJdbc.conConexionPropia("eliminar mercancía", c -> eliminar(id, c));
    }

    @Override
    public boolean delete(int id, Connection conexion) {
        return SoporteJdbc.conConexionExterna(conexion, "eliminar mercancía", c -> eliminar(id, c));
    }

    @Override
    public Optional<Mercancia> findById(int id) {
        return SoporteJdbc.conConexionPropia("buscar mercancía por id",
                c -> SoporteJdbc.buscarUno(c, SELECT_POR_ID, s -> s.setInt(1, id), MercanciaDaoImpl::mapear));
    }

    @Override
    public Optional<Mercancia> findByCodigo(String codigo) {
        return SoporteJdbc.conConexionPropia("buscar mercancía por código",
                c -> SoporteJdbc.buscarUno(c, SELECT_POR_CODIGO, s -> s.setString(1, codigo),
                        MercanciaDaoImpl::mapear));
    }

    @Override
    public List<Mercancia> findByVehiculo(int vehiculoId) {
        return SoporteJdbc.conConexionPropia("listar mercancías del vehículo",
                c -> SoporteJdbc.buscarVarios(c, SELECT_POR_VEHICULO, s -> {
                    s.setInt(1, vehiculoId);
                    s.setInt(2, vehiculoId);
                    s.setInt(3, vehiculoId);
                }, MercanciaDaoImpl::mapear));
    }

    @Override
    public List<Mercancia> findLargoRecorrido() {
        return SoporteJdbc.conConexionPropia("listar mercancías de largo recorrido",
                c -> SoporteJdbc.buscarVarios(c, SELECT_LARGO_RECORRIDO, s -> s.setBoolean(1, true),
                        MercanciaDaoImpl::mapear));
    }

    @Override
    public List<Mercancia> findAll() {
        return SoporteJdbc.conConexionPropia("listar mercancías",
                c -> SoporteJdbc.buscarVarios(c, SELECT_TODOS, SoporteJdbc.SIN_PARAMETROS, MercanciaDaoImpl::mapear));
    }

    @Override
    public boolean tieneEnvio(int mercanciaId) {
        return SoporteJdbc.conConexionPropia("comprobar envío de la mercancía",
                c -> SoporteJdbc.consultarEntero(c, TIENE_ENVIO, s -> s.setInt(1, mercanciaId)) == 1);
    }

    private static Void insertar(Mercancia mercancia, Connection conexion) throws SQLException {
        SoporteJdbc.exigirSinId(mercancia.getId(), "una mercancía");
        int id = SoporteJdbc.insertar(conexion, INSERT, s -> {
            s.setString(1, mercancia.getCodigo());
            s.setString(2, mercancia.getTipo().name());
            asignarDatos(s, 3, mercancia);
        });
        mercancia.asignarId(id);
        return null;
    }

    private static boolean actualizar(Mercancia mercancia, Connection conexion) throws SQLException {
        int id = SoporteJdbc.idPersistido(mercancia.getId(), "una mercancía");
        return SoporteJdbc.modificar(conexion, UPDATE, s -> {
            asignarDatos(s, 1, mercancia);
            s.setInt(12, id);
        }) > 0;
    }

    private static boolean eliminar(int id, Connection conexion) throws SQLException {
        return SoporteJdbc.modificar(conexion, DELETE, s -> s.setInt(1, id)) > 0;
    }

    /**
     * Asigna, desde {@code inicio}, descripcion, precio_envio, asegurada, largo_recorrido, peso, largo, ancho,
     * alto, vehiculo_id, avion_id y vehiculo_destino_id.
     */
    private static void asignarDatos(PreparedStatement sentencia, int inicio, Mercancia mercancia)
            throws SQLException {
        Sobre sobre = mercancia instanceof Sobre s ? s : null;
        Paquete paquete = mercancia instanceof Paquete p ? p : null;
        sentencia.setString(inicio, mercancia.getDescripcion());
        sentencia.setBigDecimal(inicio + 1, mercancia.getPrecioEnvio());
        sentencia.setBoolean(inicio + 2, mercancia.isAsegurada());
        sentencia.setBoolean(inicio + 3, mercancia.isLargoRecorrido());
        SoporteJdbc.asignarDecimalOpcional(sentencia, inicio + 4, sobre == null ? null : sobre.getPeso());
        SoporteJdbc.asignarDecimalOpcional(sentencia, inicio + 5, paquete == null ? null : paquete.getLargo());
        SoporteJdbc.asignarDecimalOpcional(sentencia, inicio + 6, paquete == null ? null : paquete.getAncho());
        SoporteJdbc.asignarDecimalOpcional(sentencia, inicio + 7, paquete == null ? null : paquete.getAlto());
        sentencia.setInt(inicio + 8, mercancia.getVehiculoId());
        SoporteJdbc.asignarEnteroOpcional(sentencia, inicio + 9, mercancia.getAvionId());
        SoporteJdbc.asignarEnteroOpcional(sentencia, inicio + 10, mercancia.getVehiculoDestinoId());
    }

    private static Mercancia mapear(ResultSet fila) throws SQLException {
        int id = fila.getInt("id");
        String codigo = fila.getString("codigo");
        String descripcion = fila.getString("descripcion");
        BigDecimal precioEnvio = fila.getBigDecimal("precio_envio");
        boolean asegurada = fila.getBoolean("asegurada");
        boolean largoRecorrido = fila.getBoolean("largo_recorrido");
        int vehiculoId = fila.getInt("vehiculo_id");
        Integer avionId = SoporteJdbc.leerEnteroOpcional(fila, "avion_id");
        Integer vehiculoDestinoId = SoporteJdbc.leerEnteroOpcional(fila, "vehiculo_destino_id");
        return switch (TipoMercancia.valueOf(fila.getString("tipo"))) {
            case SOBRE -> new Sobre(id, codigo, descripcion, precioEnvio, asegurada, largoRecorrido,
                    vehiculoId, avionId, vehiculoDestinoId, fila.getBigDecimal("peso"));
            case PAQUETE -> new Paquete(id, codigo, descripcion, precioEnvio, asegurada, largoRecorrido,
                    vehiculoId, avionId, vehiculoDestinoId,
                    fila.getBigDecimal("largo"), fila.getBigDecimal("ancho"), fila.getBigDecimal("alto"));
        };
    }
}
