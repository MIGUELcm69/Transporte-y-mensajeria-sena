package com.transportes.model.dao;

import com.transportes.model.Cliente;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Las sobrecargas con {@link Connection} usan la conexión del llamador para que pueda agrupar
 * varias operaciones en una transacción; nunca la cierran ni hacen commit o rollback.
 */
public interface ClienteDao {

    /** Inserta el cliente y le asigna el id generado. */
    void save(Cliente cliente);

    void save(Cliente cliente, Connection conexion);

    /**
     * Actualiza NIF/CIF, nombre, dirección y teléfono de la fila con el id de la instancia.
     * No toca código, tipo, fecha de alta ni total facturado. Devuelve false si no existe.
     */
    boolean update(Cliente cliente);

    boolean update(Cliente cliente, Connection conexion);

    boolean delete(int id);

    boolean delete(int id, Connection conexion);

    Optional<Cliente> findById(int id);

    Optional<Cliente> findByCodigo(String codigoCliente);

    List<Cliente> findAll();

    /**
     * Suma {@code delta} (negativo para restar) al total facturado con un UPDATE atómico (RF-17).
     * Devuelve false si el cliente no existe.
     */
    boolean sumarTotalFacturado(int clienteId, BigDecimal delta);

    boolean sumarTotalFacturado(int clienteId, BigDecimal delta, Connection conexion);
}
