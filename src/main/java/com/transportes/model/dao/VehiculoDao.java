package com.transportes.model.dao;

import com.transportes.model.TipoVehiculo;
import com.transportes.model.Vehiculo;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Las sobrecargas con {@link Connection} usan la conexión del llamador para que pueda agrupar
 * varias operaciones en una transacción; nunca la cierran ni hacen commit o rollback.
 */
public interface VehiculoDao {

    /** Inserta el vehículo y le asigna el id generado. */
    void save(Vehiculo vehiculo);

    void save(Vehiculo vehiculo, Connection conexion);

    /** Actualiza la fila con el id de la instancia (código y tipo no cambian); devuelve false si no existe. */
    boolean update(Vehiculo vehiculo);

    boolean update(Vehiculo vehiculo, Connection conexion);

    boolean delete(int id);

    boolean delete(int id, Connection conexion);

    Optional<Vehiculo> findById(int id);

    Optional<Vehiculo> findByCodigo(String codigo);

    List<Vehiculo> findByTipo(TipoVehiculo tipo);

    List<Vehiculo> findBySupervisor(int supervisorId);

    List<Vehiculo> findAll();

    /** Vehículos a cargo del supervisor, para RF-11 y P-01. */
    int contarPorSupervisor(int supervisorId);

    /** Si el vehículo figura en alguna mercancía como vehículo, avión o vehículo de destino (RF-06). */
    boolean tieneMercancias(int vehiculoId);
}
