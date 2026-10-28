package com.transportes.controller;

import com.transportes.exception.DatoInvalidoException;
import com.transportes.exception.EntidadNoEncontradaException;
import com.transportes.exception.SupervisorConVehiculosException;
import com.transportes.model.Supervisor;
import com.transportes.model.Vehiculo;
import com.transportes.model.dao.SupervisorDao;
import com.transportes.model.dao.VehiculoDao;

import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

public class SupervisorController {

    private final SupervisorDao supervisorDao;
    private final VehiculoDao vehiculoDao;
    private final Transacciones transacciones;

    public SupervisorController(SupervisorDao supervisorDao, VehiculoDao vehiculoDao, Transacciones transacciones) {
        this.supervisorDao = Objects.requireNonNull(supervisorDao, "supervisorDao");
        this.vehiculoDao = Objects.requireNonNull(vehiculoDao, "vehiculoDao");
        this.transacciones = Objects.requireNonNull(transacciones, "transacciones");
    }

    /**
     * RF-09 y RF-13 (P-01): guarda el supervisor y su primer vehículo en una sola transacción.
     * {@code primerVehiculo} recibe el id generado del supervisor y crea el vehículo a su cargo.
     * Tras un rollback, el supervisor ya tiene id asignado: hay que crear objetos nuevos para reintentar.
     */
    public FichaSupervisor registrar(Supervisor supervisor, IntFunction<Vehiculo> primerVehiculo) {
        Objects.requireNonNull(primerVehiculo, "primerVehiculo");
        return transacciones.ejecutar(conexion -> {
            supervisorDao.save(supervisor, conexion);
            Vehiculo vehiculo = primerVehiculo.apply(supervisor.getId());
            if (!supervisor.getId().equals(vehiculo.getSupervisorId())) {
                throw new IllegalArgumentException("El primer vehículo debe quedar a cargo del supervisor nuevo.");
            }
            vehiculoDao.save(vehiculo, conexion);
            return new FichaSupervisor(supervisor, List.of(vehiculo));
        });
    }

    /** RF-11. */
    public void modificar(Supervisor supervisor) {
        if (supervisor.getId() == null) {
            throw new IllegalArgumentException("No se puede modificar un supervisor sin id.");
        }
        if (!supervisorDao.update(supervisor)) {
            throw new EntidadNoEncontradaException("un supervisor", supervisor.getId());
        }
    }

    /** RF-11: solo supervisores sin vehículos a cargo. */
    public void eliminar(int id) {
        buscarPorId(id);
        if (vehiculoDao.contarPorSupervisor(id) > 0) {
            throw new SupervisorConVehiculosException(
                    "No se puede eliminar el supervisor porque tiene vehículos a cargo; traspáselos antes a otro supervisor.");
        }
        if (!supervisorDao.delete(id)) {
            throw new EntidadNoEncontradaException("un supervisor", id);
        }
    }

    /**
     * RF-11 y RF-13: pasa todos los vehículos del supervisor al de destino y lo elimina, en una sola transacción.
     * Devuelve el número de vehículos traspasados.
     */
    public int eliminarTraspasandoVehiculos(int id, int supervisorDestinoId) {
        if (id == supervisorDestinoId) {
            throw new DatoInvalidoException("El supervisor de destino debe ser distinto del que se elimina.");
        }
        buscarPorId(id);
        buscarPorId(supervisorDestinoId);
        return transacciones.ejecutar(conexion -> {
            int traspasados = vehiculoDao.reasignarSupervisor(id, supervisorDestinoId, conexion);
            if (!supervisorDao.delete(id, conexion)) {
                throw new EntidadNoEncontradaException("un supervisor", id);
            }
            return traspasados;
        });
    }

    public Supervisor buscarPorId(int id) {
        return supervisorDao.findById(id).orElseThrow(() -> new EntidadNoEncontradaException("un supervisor", id));
    }

    public Supervisor buscarPorNif(String nif) {
        return supervisorDao.findByNif(nif).orElseThrow(() -> new EntidadNoEncontradaException("un supervisor", nif));
    }

    /** RF-12. */
    public FichaSupervisor consultar(int id) {
        return fichaDe(buscarPorId(id));
    }

    /** RF-12. */
    public List<FichaSupervisor> listar() {
        return supervisorDao.findAll().stream().map(this::fichaDe).toList();
    }

    /** RF-10. */
    public int contarAviones(int id) {
        buscarPorId(id);
        return supervisorDao.contarAviones(id);
    }

    private FichaSupervisor fichaDe(Supervisor supervisor) {
        return new FichaSupervisor(supervisor, vehiculoDao.findBySupervisor(supervisor.getId()));
    }
}
