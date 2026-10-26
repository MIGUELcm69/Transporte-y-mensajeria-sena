package com.transportes.model.dao;

import com.transportes.model.Supervisor;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Las sobrecargas con {@link Connection} usan la conexión del llamador para que pueda agrupar
 * varias operaciones en una transacción; nunca la cierran ni hacen commit o rollback.
 */
public interface SupervisorDao {

    /** Inserta el supervisor y le asigna el id generado. */
    void save(Supervisor supervisor);

    void save(Supervisor supervisor, Connection conexion);

    /** Actualiza la fila con el id de la instancia; devuelve false si no existe. */
    boolean update(Supervisor supervisor);

    boolean update(Supervisor supervisor, Connection conexion);

    boolean delete(int id);

    boolean delete(int id, Connection conexion);

    Optional<Supervisor> findById(int id);

    Optional<Supervisor> findByNif(String nif);

    List<Supervisor> findAll();

    /** Número de aviones a cargo del supervisor (RF-10). */
    int contarAviones(int supervisorId);
}
