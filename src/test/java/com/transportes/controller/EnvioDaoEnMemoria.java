package com.transportes.controller;

import com.transportes.exception.RegistroDuplicadoException;
import com.transportes.model.Envio;
import com.transportes.model.dao.EnvioDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Envio es inmutable salvo el id, así que se guardan las mismas instancias. */
final class EnvioDaoEnMemoria implements EnvioDao {

    private final Map<Integer, Envio> filas = new TreeMap<>();
    private int siguienteId = 1;

    /** Inserta directamente, como haría seed.sql. */
    Envio sembrar(int clienteOrigenId, int destinatarioId, int mercanciaId, LocalDate fecha, String precioTotal) {
        Envio envio = new Envio(null, clienteOrigenId, destinatarioId, mercanciaId, fecha, 0,
                new BigDecimal(precioTotal));
        guardar(envio);
        return envio;
    }

    boolean figuraCliente(int clienteId) {
        return filas.values().stream()
                .anyMatch(e -> e.getClienteOrigenId() == clienteId || e.getDestinatarioId() == clienteId);
    }

    boolean figuraMercancia(int mercanciaId) {
        return filas.values().stream().anyMatch(e -> e.getMercanciaId() == mercanciaId);
    }

    @Override
    public void save(Envio envio) {
        guardar(envio);
    }

    @Override
    public void save(Envio envio, Connection conexion) {
        Objects.requireNonNull(conexion);
        guardar(envio);
    }

    @Override
    public boolean update(Envio envio) {
        return filas.replace(envio.getId(), envio) != null;
    }

    @Override
    public boolean update(Envio envio, Connection conexion) {
        Objects.requireNonNull(conexion);
        return update(envio);
    }

    @Override
    public boolean delete(int id) {
        return filas.remove(id) != null;
    }

    @Override
    public boolean delete(int id, Connection conexion) {
        Objects.requireNonNull(conexion);
        return delete(id);
    }

    @Override
    public Optional<Envio> findById(int id) {
        return Optional.ofNullable(filas.get(id));
    }

    @Override
    public List<Envio> findByCliente(int clienteOrigenId) {
        return filas.values().stream().filter(e -> e.getClienteOrigenId() == clienteOrigenId)
                .sorted(Comparator.comparing(Envio::getFechaEnvio).thenComparing(Envio::getId)).toList();
    }

    @Override
    public List<Envio> findAll() {
        return List.copyOf(filas.values());
    }

    @Override
    public boolean actualizarIntentos(Envio envio) {
        return update(envio);
    }

    @Override
    public boolean actualizarIntentos(Envio envio, Connection conexion) {
        Objects.requireNonNull(conexion);
        return update(envio);
    }

    private void guardar(Envio envio) {
        if (figuraMercancia(envio.getMercanciaId())) {
            throw new RegistroDuplicadoException("La mercancía ya tiene un envío asociado.");
        }
        envio.asignarId(siguienteId++);
        filas.put(envio.getId(), envio);
    }
}
