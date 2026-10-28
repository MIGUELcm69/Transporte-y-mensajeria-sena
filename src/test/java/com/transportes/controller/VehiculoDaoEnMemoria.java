package com.transportes.controller;

import com.transportes.exception.RegistroDuplicadoException;
import com.transportes.model.Avion;
import com.transportes.model.Furgoneta;
import com.transportes.model.Moto;
import com.transportes.model.TipoVehiculo;
import com.transportes.model.Vehiculo;
import com.transportes.model.dao.VehiculoDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Guarda y devuelve copias, como la BD: modificar un objeto leído no cambia la fila hasta llamar a update.
 * Las escrituras sin {@link Connection} se cuentan para detectar las hechas fuera de una transacción.
 */
final class VehiculoDaoEnMemoria implements VehiculoDao, ConInstantanea {

    static final LocalDate FECHA = LocalDate.of(2026, 1, 15);
    static final BigDecimal PRECIO = new BigDecimal("1000.00");

    private final Map<Integer, Vehiculo> filas = new TreeMap<>();
    private final Set<Integer> conMercancias = new HashSet<>();
    private int siguienteId = 1;
    int escriturasSinTransaccion;

    static Vehiculo moto(String codigo, int supervisorId) {
        return new Moto(null, codigo, "Moto " + codigo, FECHA, PRECIO, supervisorId, new BigDecimal("125.00"));
    }

    static Vehiculo avion(String codigo, int supervisorId) {
        return new Avion(null, codigo, "Avión " + codigo, FECHA, PRECIO, supervisorId);
    }

    /** Inserta directamente, como haría seed.sql. */
    Vehiculo sembrar(Vehiculo vehiculo) {
        guardar(vehiculo);
        return vehiculo;
    }

    void marcarConMercancias(int vehiculoId) {
        conMercancias.add(vehiculoId);
    }

    @Override
    public Runnable instantanea() {
        Map<Integer, Vehiculo> copia = new TreeMap<>(filas);
        int id = siguienteId;
        return () -> {
            filas.clear();
            filas.putAll(copia);
            siguienteId = id;
        };
    }

    @Override
    public void save(Vehiculo vehiculo) {
        escriturasSinTransaccion++;
        guardar(vehiculo);
    }

    @Override
    public void save(Vehiculo vehiculo, Connection conexion) {
        Objects.requireNonNull(conexion);
        guardar(vehiculo);
    }

    @Override
    public boolean update(Vehiculo vehiculo) {
        escriturasSinTransaccion++;
        return actualizar(vehiculo);
    }

    @Override
    public boolean update(Vehiculo vehiculo, Connection conexion) {
        Objects.requireNonNull(conexion);
        return actualizar(vehiculo);
    }

    @Override
    public int reasignarSupervisor(int origenId, int destinoId, Connection conexion) {
        Objects.requireNonNull(conexion);
        List<Vehiculo> aMover = findBySupervisor(origenId);
        for (Vehiculo vehiculo : aMover) {
            vehiculo.setSupervisorId(destinoId);
            filas.put(vehiculo.getId(), vehiculo);
        }
        return aMover.size();
    }

    @Override
    public boolean delete(int id) {
        escriturasSinTransaccion++;
        return filas.remove(id) != null;
    }

    @Override
    public boolean delete(int id, Connection conexion) {
        Objects.requireNonNull(conexion);
        return filas.remove(id) != null;
    }

    @Override
    public Optional<Vehiculo> findById(int id) {
        return Optional.ofNullable(filas.get(id)).map(VehiculoDaoEnMemoria::copiar);
    }

    @Override
    public Optional<Vehiculo> findByCodigo(String codigo) {
        return filas.values().stream().filter(v -> v.getCodigo().equals(codigo)).findFirst()
                .map(VehiculoDaoEnMemoria::copiar);
    }

    @Override
    public List<Vehiculo> findByTipo(TipoVehiculo tipo) {
        return filas.values().stream().filter(v -> v.getTipo() == tipo).map(VehiculoDaoEnMemoria::copiar).toList();
    }

    @Override
    public List<Vehiculo> findBySupervisor(int supervisorId) {
        return filas.values().stream().filter(v -> v.getSupervisorId() == supervisorId)
                .map(VehiculoDaoEnMemoria::copiar).toList();
    }

    @Override
    public List<Vehiculo> findAll() {
        return filas.values().stream().map(VehiculoDaoEnMemoria::copiar).toList();
    }

    @Override
    public int contarPorSupervisor(int supervisorId) {
        return findBySupervisor(supervisorId).size();
    }

    @Override
    public boolean tieneMercancias(int vehiculoId) {
        return conMercancias.contains(vehiculoId);
    }

    private void guardar(Vehiculo vehiculo) {
        if (filas.values().stream().anyMatch(v -> v.getCodigo().equals(vehiculo.getCodigo()))) {
            throw new RegistroDuplicadoException("Ya existe un vehículo con ese código.");
        }
        vehiculo.asignarId(siguienteId++);
        filas.put(vehiculo.getId(), copiar(vehiculo));
    }

    private boolean actualizar(Vehiculo vehiculo) {
        return filas.replace(vehiculo.getId(), copiar(vehiculo)) != null;
    }

    private static Vehiculo copiar(Vehiculo v) {
        return switch (v.getTipo()) {
            case MOTO -> new Moto(v.getId(), v.getCodigo(), v.getDescripcion(), v.getFechaCompra(),
                    v.getPrecioCompra(), v.getSupervisorId(), ((Moto) v).getCilindrada());
            case FURGONETA -> new Furgoneta(v.getId(), v.getCodigo(), v.getDescripcion(), v.getFechaCompra(),
                    v.getPrecioCompra(), v.getSupervisorId(), ((Furgoneta) v).getCapacidad());
            case AVION -> new Avion(v.getId(), v.getCodigo(), v.getDescripcion(), v.getFechaCompra(),
                    v.getPrecioCompra(), v.getSupervisorId());
        };
    }
}
