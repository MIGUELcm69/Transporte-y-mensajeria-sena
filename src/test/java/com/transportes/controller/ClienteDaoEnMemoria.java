package com.transportes.controller;

import com.transportes.exception.RegistroDuplicadoException;
import com.transportes.model.Cliente;
import com.transportes.model.ClienteEmpresa;
import com.transportes.model.ClienteParticular;
import com.transportes.model.dao.ClienteDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Guarda y devuelve copias, como la BD: modificar un objeto leído no cambia la fila hasta llamar a update.
 * Como ClienteDaoImpl, update no toca código, tipo, fecha de alta ni total facturado.
 * {@link #sumasDeTotal} cuenta las llamadas a sumarTotalFacturado.
 */
final class ClienteDaoEnMemoria implements ClienteDao, ConInstantanea {

    static final LocalDate FECHA_ALTA = LocalDate.of(2026, 2, 1);

    private final Map<Integer, Cliente> filas = new TreeMap<>();
    private final EnvioDaoEnMemoria envios;
    private int siguienteId = 1;
    int sumasDeTotal;

    ClienteDaoEnMemoria(EnvioDaoEnMemoria envios) {
        this.envios = envios;
    }

    static ClienteParticular particular(String codigo, String nif) {
        return ClienteParticular.nuevo(codigo, FECHA_ALTA, nif, "Particular " + codigo, "Calle 3");
    }

    static ClienteEmpresa empresa(String codigo, String cif) {
        return ClienteEmpresa.nuevo(codigo, FECHA_ALTA, cif, "Empresa " + codigo, "Calle 4", "910000000");
    }

    /** Inserta directamente, como haría seed.sql. */
    <C extends Cliente> C sembrar(C cliente) {
        guardar(cliente);
        return cliente;
    }

    @Override
    public Runnable instantanea() {
        Map<Integer, Cliente> copia = new TreeMap<>(filas);
        int id = siguienteId;
        return () -> {
            filas.clear();
            filas.putAll(copia);
            siguienteId = id;
        };
    }

    @Override
    public void save(Cliente cliente) {
        guardar(cliente);
    }

    @Override
    public void save(Cliente cliente, Connection conexion) {
        Objects.requireNonNull(conexion);
        guardar(cliente);
    }

    @Override
    public boolean update(Cliente cliente) {
        Cliente actual = filas.get(cliente.getId());
        if (actual == null) {
            return false;
        }
        filas.put(cliente.getId(), copiar(cliente, actual.getFechaAlta(), actual.getTotalFacturado()));
        return true;
    }

    @Override
    public boolean update(Cliente cliente, Connection conexion) {
        Objects.requireNonNull(conexion);
        return update(cliente);
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
    public Optional<Cliente> findById(int id) {
        return Optional.ofNullable(filas.get(id)).map(ClienteDaoEnMemoria::copiar);
    }

    @Override
    public Optional<Cliente> findByCodigo(String codigoCliente) {
        return filas.values().stream().filter(c -> c.getCodigoCliente().equals(codigoCliente)).findFirst()
                .map(ClienteDaoEnMemoria::copiar);
    }

    @Override
    public List<Cliente> findAll() {
        return filas.values().stream().map(ClienteDaoEnMemoria::copiar).toList();
    }

    @Override
    public boolean sumarTotalFacturado(int clienteId, BigDecimal delta) {
        sumasDeTotal++;
        Cliente actual = filas.get(clienteId);
        if (actual == null) {
            return false;
        }
        filas.put(clienteId, copiar(actual, actual.getFechaAlta(), actual.getTotalFacturado().add(delta)));
        return true;
    }

    @Override
    public boolean sumarTotalFacturado(int clienteId, BigDecimal delta, Connection conexion) {
        Objects.requireNonNull(conexion);
        return sumarTotalFacturado(clienteId, delta);
    }

    @Override
    public boolean tieneEnvios(int clienteId) {
        return envios.figuraCliente(clienteId);
    }

    private void guardar(Cliente cliente) {
        if (filas.values().stream().anyMatch(c -> c.getCodigoCliente().equals(cliente.getCodigoCliente()))) {
            throw new RegistroDuplicadoException("Ya existe un cliente con ese código.");
        }
        cliente.asignarId(siguienteId++);
        filas.put(cliente.getId(), copiar(cliente));
    }

    private static Cliente copiar(Cliente c) {
        return copiar(c, c.getFechaAlta(), c.getTotalFacturado());
    }

    private static Cliente copiar(Cliente c, LocalDate fechaAlta, BigDecimal totalFacturado) {
        return switch (c.getTipo()) {
            case PARTICULAR -> new ClienteParticular(c.getId(), c.getCodigoCliente(), fechaAlta, totalFacturado,
                    ((ClienteParticular) c).getNif(), c.getNombre(), c.getDireccion());
            case EMPRESA -> new ClienteEmpresa(c.getId(), c.getCodigoCliente(), fechaAlta, totalFacturado,
                    ((ClienteEmpresa) c).getCif(), c.getNombre(), c.getDireccion(), ((ClienteEmpresa) c).getTelefono());
        };
    }
}
