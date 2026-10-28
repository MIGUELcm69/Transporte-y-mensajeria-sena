package com.transportes.controller;

import com.transportes.exception.DatoInvalidoException;
import com.transportes.exception.EntidadNoEncontradaException;
import com.transportes.exception.UltimoVehiculoException;
import com.transportes.exception.VehiculoEnUsoException;
import com.transportes.model.TipoVehiculo;
import com.transportes.model.Vehiculo;
import com.transportes.model.dao.SupervisorDao;
import com.transportes.model.dao.VehiculoDao;

import java.util.List;
import java.util.Objects;

public class VehiculoController {

    private final VehiculoDao vehiculoDao;
    private final SupervisorDao supervisorDao;

    public VehiculoController(VehiculoDao vehiculoDao, SupervisorDao supervisorDao) {
        this.vehiculoDao = Objects.requireNonNull(vehiculoDao, "vehiculoDao");
        this.supervisorDao = Objects.requireNonNull(supervisorDao, "supervisorDao");
    }

    /** Registra un vehículo para un supervisor existente; el supervisor nuevo se crea con SupervisorController. */
    public Vehiculo registrar(Vehiculo vehiculo) {
        exigirSupervisor(vehiculo.getSupervisorId());
        vehiculoDao.save(vehiculo);
        return vehiculo;
    }

    /** RF-05. Si cambia de supervisor, el anterior no puede quedarse sin vehículos (RF-13). */
    public void modificar(Vehiculo vehiculo) {
        Vehiculo actual = buscarPorId(idPersistido(vehiculo));
        if (!actual.getCodigo().equals(vehiculo.getCodigo()) || actual.getTipo() != vehiculo.getTipo()) {
            throw new DatoInvalidoException("El código y el tipo de un vehículo no se pueden modificar.");
        }
        if (actual.getSupervisorId() != vehiculo.getSupervisorId()) {
            exigirSupervisor(vehiculo.getSupervisorId());
            if (vehiculoDao.contarPorSupervisor(actual.getSupervisorId()) <= 1) {
                throw new UltimoVehiculoException(
                        "No se puede reasignar el vehículo: es el único a cargo de su supervisor.");
            }
        }
        if (!vehiculoDao.update(vehiculo)) {
            throw new EntidadNoEncontradaException("un vehículo", vehiculo.getId());
        }
    }

    /** RF-06 y RF-13. */
    public void eliminar(int id) {
        Vehiculo actual = buscarPorId(id);
        if (vehiculoDao.tieneMercancias(id)) {
            throw new VehiculoEnUsoException("No se puede eliminar el vehículo porque tiene mercancías asignadas.");
        }
        if (vehiculoDao.contarPorSupervisor(actual.getSupervisorId()) <= 1) {
            throw new UltimoVehiculoException(
                    "No se puede eliminar el vehículo: es el único a cargo de su supervisor.");
        }
        if (!vehiculoDao.delete(id)) {
            throw new EntidadNoEncontradaException("un vehículo", id);
        }
    }

    public Vehiculo buscarPorId(int id) {
        return vehiculoDao.findById(id).orElseThrow(() -> new EntidadNoEncontradaException("un vehículo", id));
    }

    /** RF-07. */
    public Vehiculo buscarPorCodigo(String codigo) {
        return vehiculoDao.findByCodigo(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("un vehículo", codigo));
    }

    /** RF-07. */
    public List<Vehiculo> listarPorTipo(TipoVehiculo tipo) {
        return vehiculoDao.findByTipo(Objects.requireNonNull(tipo, "tipo"));
    }

    public List<Vehiculo> listarTodos() {
        return vehiculoDao.findAll();
    }

    private void exigirSupervisor(int supervisorId) {
        if (supervisorDao.findById(supervisorId).isEmpty()) {
            throw new EntidadNoEncontradaException("un supervisor", supervisorId);
        }
    }

    private static int idPersistido(Vehiculo vehiculo) {
        if (vehiculo.getId() == null) {
            throw new IllegalArgumentException("No se puede modificar un vehículo sin id.");
        }
        return vehiculo.getId();
    }
}
