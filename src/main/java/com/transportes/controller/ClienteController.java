package com.transportes.controller;

import com.transportes.exception.ClienteConEnviosException;
import com.transportes.exception.DatoInvalidoException;
import com.transportes.exception.EntidadNoEncontradaException;
import com.transportes.model.Cliente;
import com.transportes.model.ClienteEmpresa;
import com.transportes.model.ClienteParticular;
import com.transportes.model.dao.ClienteDao;
import com.transportes.model.dao.EnvioDao;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class ClienteController {

    private final ClienteDao clienteDao;
    private final EnvioDao envioDao;
    private final Clock reloj;

    public ClienteController(ClienteDao clienteDao, EnvioDao envioDao) {
        this(clienteDao, envioDao, Clock.systemDefaultZone());
    }

    /** {@code reloj} fija la fecha de alta de los clientes nuevos (P-07). */
    public ClienteController(ClienteDao clienteDao, EnvioDao envioDao, Clock reloj) {
        this.clienteDao = Objects.requireNonNull(clienteDao, "clienteDao");
        this.envioDao = Objects.requireNonNull(envioDao, "envioDao");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /** RF-14 y RF-15: fecha de alta de hoy y total facturado 0. */
    public ClienteParticular registrarParticular(String codigoCliente, String nif, String nombre, String direccion) {
        ClienteParticular cliente = ClienteParticular.nuevo(codigoCliente, hoy(), nif, nombre, direccion);
        clienteDao.save(cliente);
        return cliente;
    }

    /** RF-14 y RF-15: fecha de alta de hoy y total facturado 0. */
    public ClienteEmpresa registrarEmpresa(String codigoCliente, String cif, String nombre, String direccion,
                                           String telefono) {
        ClienteEmpresa cliente = ClienteEmpresa.nuevo(codigoCliente, hoy(), cif, nombre, direccion, telefono);
        clienteDao.save(cliente);
        return cliente;
    }

    /** RF-16. Solo cambian NIF/CIF, nombre, dirección y teléfono. */
    public void modificar(Cliente cliente) {
        Cliente actual = buscarPorId(idPersistido(cliente));
        if (actual.getTipo() != cliente.getTipo()) {
            throw new DatoInvalidoException(
                    "El tipo de un cliente no se puede modificar; para cambiarlo hay que registrar un cliente nuevo.");
        }
        if (!actual.getCodigoCliente().equals(cliente.getCodigoCliente())) {
            throw new DatoInvalidoException("El código de un cliente no se puede modificar.");
        }
        if (!clienteDao.update(cliente)) {
            throw new EntidadNoEncontradaException("un cliente", cliente.getId());
        }
    }

    /** RF-16: solo clientes que no figuran en ningún envío, ni como origen ni como destinatario. */
    public void eliminar(int id) {
        buscarPorId(id);
        if (clienteDao.tieneEnvios(id)) {
            throw new ClienteConEnviosException(
                    "No se puede eliminar el cliente porque figura en envíos como origen o como destinatario.");
        }
        if (!clienteDao.delete(id)) {
            throw new EntidadNoEncontradaException("un cliente", id);
        }
    }

    public Cliente buscarPorId(int id) {
        return clienteDao.findById(id).orElseThrow(() -> new EntidadNoEncontradaException("un cliente", id));
    }

    /** RF-16. */
    public Cliente buscarPorCodigo(String codigoCliente) {
        return clienteDao.findByCodigo(codigoCliente)
                .orElseThrow(() -> new EntidadNoEncontradaException("un cliente", codigoCliente));
    }

    /** RF-16. */
    public List<Cliente> listarTodos() {
        return clienteDao.findAll();
    }

    /** RF-18 y RF-36. */
    public HistorialCliente historial(int id) {
        return new HistorialCliente(buscarPorId(id), envioDao.findByCliente(id));
    }

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    private static int idPersistido(Cliente cliente) {
        if (cliente.getId() == null) {
            throw new IllegalArgumentException("No se puede modificar un cliente sin id.");
        }
        return cliente.getId();
    }
}
