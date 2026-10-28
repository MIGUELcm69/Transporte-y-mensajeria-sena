package com.transportes.controller;

import com.transportes.exception.RegistroDuplicadoException;
import com.transportes.model.Supervisor;
import com.transportes.model.TipoVehiculo;
import com.transportes.model.dao.SupervisorDao;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Supervisor es inmutable salvo el id, así que se guardan las mismas instancias.
 * Las escrituras sin {@link Connection} se cuentan para detectar las hechas fuera de una transacción.
 */
final class SupervisorDaoEnMemoria implements SupervisorDao, ConInstantanea {

    private final Map<Integer, Supervisor> filas = new TreeMap<>();
    private VehiculoDaoEnMemoria vehiculos;
    private int siguienteId = 1;
    int escriturasSinTransaccion;
    boolean fallarAlEliminar;

    /** Necesario solo para {@link #contarAviones(int)}. */
    void usarVehiculos(VehiculoDaoEnMemoria vehiculos) {
        this.vehiculos = vehiculos;
    }

    /** Inserta directamente, como haría seed.sql. */
    Supervisor sembrar(String nif) {
        Supervisor supervisor = new Supervisor(null, nif, "Supervisor " + nif, "Calle 1", "600000000");
        guardar(supervisor);
        return supervisor;
    }

    @Override
    public Runnable instantanea() {
        Map<Integer, Supervisor> copia = new TreeMap<>(filas);
        int id = siguienteId;
        return () -> {
            filas.clear();
            filas.putAll(copia);
            siguienteId = id;
        };
    }

    @Override
    public void save(Supervisor supervisor) {
        escriturasSinTransaccion++;
        guardar(supervisor);
    }

    @Override
    public void save(Supervisor supervisor, Connection conexion) {
        Objects.requireNonNull(conexion);
        guardar(supervisor);
    }

    @Override
    public boolean update(Supervisor supervisor) {
        escriturasSinTransaccion++;
        return actualizar(supervisor);
    }

    @Override
    public boolean update(Supervisor supervisor, Connection conexion) {
        Objects.requireNonNull(conexion);
        return actualizar(supervisor);
    }

    @Override
    public boolean delete(int id) {
        escriturasSinTransaccion++;
        return eliminar(id);
    }

    @Override
    public boolean delete(int id, Connection conexion) {
        Objects.requireNonNull(conexion);
        return eliminar(id);
    }

    @Override
    public Optional<Supervisor> findById(int id) {
        return Optional.ofNullable(filas.get(id));
    }

    @Override
    public Optional<Supervisor> findByNif(String nif) {
        return filas.values().stream().filter(s -> s.getNif().equals(nif)).findFirst();
    }

    @Override
    public List<Supervisor> findAll() {
        return List.copyOf(filas.values());
    }

    @Override
    public int contarAviones(int supervisorId) {
        return (int) vehiculos.findBySupervisor(supervisorId).stream()
                .filter(v -> v.getTipo() == TipoVehiculo.AVION).count();
    }

    private void guardar(Supervisor supervisor) {
        if (findByNif(supervisor.getNif()).isPresent()) {
            throw new RegistroDuplicadoException("Ya existe un supervisor con ese NIF.");
        }
        supervisor.asignarId(siguienteId++);
        filas.put(supervisor.getId(), supervisor);
    }

    private boolean actualizar(Supervisor supervisor) {
        return filas.replace(supervisor.getId(), supervisor) != null;
    }

    private boolean eliminar(int id) {
        if (fallarAlEliminar) {
            throw new IllegalStateException("Fallo simulado al eliminar el supervisor.");
        }
        return filas.remove(id) != null;
    }
}
