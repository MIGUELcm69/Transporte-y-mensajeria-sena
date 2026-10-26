package com.transportes.model.dao;

import com.transportes.model.Mercancia;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Las sobrecargas con {@link Connection} usan la conexión del llamador para que pueda agrupar
 * varias operaciones en una transacción; nunca la cierran ni hacen commit o rollback.
 */
public interface MercanciaDao {

    /** Inserta la mercancía y le asigna el id generado. */
    void save(Mercancia mercancia);

    void save(Mercancia mercancia, Connection conexion);

    /** Actualiza la fila con el id de la instancia (código y tipo no cambian); devuelve false si no existe. */
    boolean update(Mercancia mercancia);

    boolean update(Mercancia mercancia, Connection conexion);

    boolean delete(int id);

    boolean delete(int id, Connection conexion);

    Optional<Mercancia> findById(int id);

    Optional<Mercancia> findByCodigo(String codigo);

    /** Mercancías en las que el vehículo figura como vehículo, avión o vehículo de destino (RF-35). */
    List<Mercancia> findByVehiculo(int vehiculoId);

    List<Mercancia> findLargoRecorrido();

    List<Mercancia> findAll();

    /** Si la mercancía ya tiene un envío asociado (RF-26). */
    boolean tieneEnvio(int mercanciaId);
}
