package com.transportes.controller;

import com.transportes.exception.DatoInvalidoException;
import com.transportes.exception.EntidadNoEncontradaException;
import com.transportes.exception.MercanciaConEnvioException;
import com.transportes.exception.ReglaAsignacionException;
import com.transportes.model.Mercancia;
import com.transportes.model.TipoVehiculo;
import com.transportes.model.Vehiculo;
import com.transportes.model.dao.MercanciaDao;
import com.transportes.model.dao.VehiculoDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MercanciaController {

    private static final String PRECIO_BLOQUEADO =
            "No se puede modificar el precio de envío porque la mercancía ya tiene un envío asociado.";

    private final MercanciaDao mercanciaDao;
    private final VehiculoDao vehiculoDao;

    public MercanciaController(MercanciaDao mercanciaDao, VehiculoDao vehiculoDao) {
        this.mercanciaDao = Objects.requireNonNull(mercanciaDao, "mercanciaDao");
        this.vehiculoDao = Objects.requireNonNull(vehiculoDao, "vehiculoDao");
    }

    /** RF-19 a RF-25. */
    public Mercancia registrar(Mercancia mercancia) {
        validarVehiculos(mercancia);
        mercanciaDao.save(mercancia);
        return mercancia;
    }

    /** RF-26. Con un envío asociado solo queda bloqueado el precio de envío. */
    public void modificar(Mercancia mercancia) {
        Mercancia actual = buscarPorId(idPersistido(mercancia));
        if (!actual.getCodigo().equals(mercancia.getCodigo()) || actual.getTipo() != mercancia.getTipo()) {
            throw new DatoInvalidoException("El código y el tipo de una mercancía no se pueden modificar.");
        }
        if (actual.getPrecioEnvio().compareTo(mercancia.getPrecioEnvio()) != 0
                && mercanciaDao.tieneEnvio(mercancia.getId())) {
            throw new MercanciaConEnvioException(PRECIO_BLOQUEADO);
        }
        validarVehiculos(mercancia);
        if (!mercanciaDao.update(mercancia)) {
            throw new EntidadNoEncontradaException("una mercancía", mercancia.getId());
        }
    }

    /** RF-26. */
    public void eliminar(int id) {
        buscarPorId(id);
        if (mercanciaDao.tieneEnvio(id)) {
            throw new MercanciaConEnvioException(
                    "No se puede eliminar la mercancía porque tiene un envío asociado.");
        }
        if (!mercanciaDao.delete(id)) {
            throw new EntidadNoEncontradaException("una mercancía", id);
        }
    }

    public Mercancia buscarPorId(int id) {
        return mercanciaDao.findById(id).orElseThrow(() -> new EntidadNoEncontradaException("una mercancía", id));
    }

    /** RF-26. */
    public Mercancia buscarPorCodigo(String codigo) {
        return mercanciaDao.findByCodigo(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("una mercancía", codigo));
    }

    /** RF-26. */
    public List<Mercancia> listarTodos() {
        return mercanciaDao.findAll();
    }

    /** RF-27 y P-06: las mercancías que aún se pueden asignar a un envío. */
    public List<Mercancia> listarSinEnvio() {
        return mercanciaDao.findAll().stream().filter(m -> !mercanciaDao.tieneEnvio(m.getId())).toList();
    }

    /** RF-26: el motivo por el que el precio de envío no se puede modificar, o vacío si se puede. */
    public Optional<String> bloqueoPrecioEnvio(int id) {
        buscarPorId(id);
        return mercanciaDao.tieneEnvio(id) ? Optional.of(PRECIO_BLOQUEADO) : Optional.empty();
    }

    /** RF-35: mercancías en las que el vehículo figura como vehículo, avión o vehículo de destino. */
    public List<Mercancia> listarPorVehiculo(int vehiculoId) {
        if (vehiculoDao.findById(vehiculoId).isEmpty()) {
            throw new EntidadNoEncontradaException("un vehículo", vehiculoId);
        }
        return mercanciaDao.findByVehiculo(vehiculoId);
    }

    /** RF-37. */
    public List<Mercancia> listarLargoRecorrido() {
        return mercanciaDao.findLargoRecorrido();
    }

    /**
     * RF-21, RF-24 y RF-25, antes de llegar a los triggers para dar un mensaje claro.
     * Que largo recorrido exija avión y vehículo de destino ya lo garantiza Mercancia (RF-23).
     */
    private void validarVehiculos(Mercancia mercancia) {
        TipoVehiculo terrestre = mercancia.getTipo().vehiculoTerrestre();
        exigirTipo(mercancia.getVehiculoId(), terrestre, "Una mercancía de tipo " + mercancia.getTipo()
                + " debe asignarse a un vehículo de tipo " + terrestre + ".");
        if (mercancia.getAvionId() != null) {
            exigirTipo(mercancia.getAvionId(), TipoVehiculo.AVION,
                    "La mercancía de largo recorrido debe asignarse a un avión.");
        }
        if (mercancia.getVehiculoDestinoId() != null) {
            exigirTipo(mercancia.getVehiculoDestinoId(), terrestre, "El vehículo de destino de una mercancía de tipo "
                    + mercancia.getTipo() + " debe ser de tipo " + terrestre + ".");
        }
    }

    private void exigirTipo(int vehiculoId, TipoVehiculo tipo, String mensaje) {
        Vehiculo vehiculo = vehiculoDao.findById(vehiculoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("un vehículo", vehiculoId));
        if (vehiculo.getTipo() != tipo) {
            throw new ReglaAsignacionException(mensaje);
        }
    }

    private static int idPersistido(Mercancia mercancia) {
        if (mercancia.getId() == null) {
            throw new IllegalArgumentException("No se puede modificar una mercancía sin id.");
        }
        return mercancia.getId();
    }
}
