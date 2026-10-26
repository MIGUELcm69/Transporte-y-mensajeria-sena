package com.transportes.model.dao;

import com.transportes.model.Envio;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Las sobrecargas con {@link Connection} usan la conexión del llamador para que pueda agrupar
 * varias operaciones en una transacción; nunca la cierran ni hacen commit o rollback.
 */
public interface EnvioDao {

    /** Inserta el envío y le asigna el id generado. */
    void save(Envio envio);

    void save(Envio envio, Connection conexion);

    /** Actualiza todos los datos de la fila con el id de la instancia; devuelve false si no existe. */
    boolean update(Envio envio);

    boolean update(Envio envio, Connection conexion);

    boolean delete(int id);

    boolean delete(int id, Connection conexion);

    Optional<Envio> findById(int id);

    /** Envíos encargados por el cliente origen, por fecha (RF-18, RF-36). */
    List<Envio> findByCliente(int clienteOrigenId);

    List<Envio> findAll();

    /** Guarda solo intentos fallidos y precio total del envío (RF-33); devuelve false si no existe. */
    boolean actualizarIntentos(Envio envio);

    boolean actualizarIntentos(Envio envio, Connection conexion);
}
