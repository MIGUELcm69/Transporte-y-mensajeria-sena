package com.transportes.controller;

import com.transportes.exception.DatoInvalidoException;
import com.transportes.exception.EntidadNoEncontradaException;
import com.transportes.exception.MercanciaConEnvioException;
import com.transportes.model.Cliente;
import com.transportes.model.Envio;
import com.transportes.model.Mercancia;
import com.transportes.model.TipoCliente;
import com.transportes.model.dao.ClienteDao;
import com.transportes.model.dao.EnvioDao;
import com.transportes.model.dao.MercanciaDao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Cada escritura de un envío y el ajuste de total facturado del cliente origen van en la misma transacción (RF-17).
 * El plus y el precio total los calcula Envio; aquí solo se suman o restan diferencias ya calculadas.
 */
public class EnvioController {

    private final EnvioDao envioDao;
    private final ClienteDao clienteDao;
    private final MercanciaDao mercanciaDao;
    private final Transacciones transacciones;

    public EnvioController(EnvioDao envioDao, ClienteDao clienteDao, MercanciaDao mercanciaDao,
                           Transacciones transacciones) {
        this.envioDao = Objects.requireNonNull(envioDao, "envioDao");
        this.clienteDao = Objects.requireNonNull(clienteDao, "clienteDao");
        this.mercanciaDao = Objects.requireNonNull(mercanciaDao, "mercanciaDao");
        this.transacciones = Objects.requireNonNull(transacciones, "transacciones");
    }

    /** RF-27 a RF-32 y RF-17: suma el precio total del envío al cliente origen. */
    public Envio crear(int clienteOrigenId, int destinatarioId, int mercanciaId, LocalDate fechaEnvio) {
        exigirClientesDistintos(clienteOrigenId, destinatarioId);
        buscarCliente(clienteOrigenId);
        Cliente destinatario = buscarCliente(destinatarioId);
        Mercancia mercancia = buscarMercancia(mercanciaId);
        exigirMercanciaSinEnvio(mercanciaId);
        Envio envio = Envio.nuevo(clienteOrigenId, destinatarioId, mercanciaId, fechaEnvio,
                mercancia.getPrecioEnvio(), destinatario.getTipo());
        return transacciones.ejecutar(conexion -> {
            envioDao.save(envio, conexion);
            sumarAlCliente(clienteOrigenId, envio.getPrecioTotal(), conexion);
            return envio;
        });
    }

    /**
     * RF-34 y RF-17. Conserva los intentos fallidos y recalcula el precio total con ellos.
     * Si cambia el cliente origen, se resta el precio anterior al antiguo y se suma el nuevo al nuevo;
     * si no, se suma la diferencia.
     */
    public Envio modificar(int id, int clienteOrigenId, int destinatarioId, int mercanciaId, LocalDate fechaEnvio) {
        exigirClientesDistintos(clienteOrigenId, destinatarioId);
        buscarCliente(clienteOrigenId);
        Cliente destinatario = buscarCliente(destinatarioId);
        Mercancia mercancia = buscarMercancia(mercanciaId);
        return transacciones.ejecutar(conexion -> {
            Envio actual = buscarPorId(id, conexion);
            if (actual.getMercanciaId() != mercanciaId) {
                exigirMercanciaSinEnvio(mercanciaId);
            }
            Envio modificado = actual.conDatos(clienteOrigenId, destinatarioId, mercanciaId, fechaEnvio,
                    mercancia.getPrecioEnvio(), destinatario.getTipo());
            if (!envioDao.update(modificado, conexion)) {
                throw new EntidadNoEncontradaException("un envío", id);
            }
            if (actual.getClienteOrigenId() == clienteOrigenId) {
                sumarAlCliente(clienteOrigenId, modificado.getPrecioTotal().subtract(actual.getPrecioTotal()),
                        conexion);
            } else {
                sumarAlCliente(actual.getClienteOrigenId(), actual.getPrecioTotal().negate(), conexion);
                sumarAlCliente(clienteOrigenId, modificado.getPrecioTotal(), conexion);
            }
            return modificado;
        });
    }

    /** RF-34 y RF-17: resta el precio total del envío al cliente origen. */
    public void eliminar(int id) {
        transacciones.ejecutar(conexion -> {
            Envio actual = buscarPorId(id, conexion);
            if (!envioDao.delete(id, conexion)) {
                throw new EntidadNoEncontradaException("un envío", id);
            }
            sumarAlCliente(actual.getClienteOrigenId(), actual.getPrecioTotal().negate(), conexion);
            return null;
        });
    }

    /** RF-30, RF-33 y RF-17: solo en envíos a particulares; suma al cliente origen el aumento del plus. */
    public Envio registrarIntentoFallido(int id) {
        return transacciones.ejecutar(conexion -> {
            Envio actual = buscarPorId(id, conexion);
            Cliente destinatario = buscarCliente(actual.getDestinatarioId());
            // RF-26 impide cambiar el precio de envío de una mercancía con envío: es el mismo que al crearlo.
            Mercancia mercancia = buscarMercancia(actual.getMercanciaId());
            Envio conIntento = actual.conIntentoFallido(mercancia.getPrecioEnvio(), destinatario.getTipo());
            if (!envioDao.actualizarIntentos(conIntento, conexion)) {
                throw new EntidadNoEncontradaException("un envío", id);
            }
            sumarAlCliente(actual.getClienteOrigenId(),
                    conIntento.getPrecioTotal().subtract(actual.getPrecioTotal()), conexion);
            return conIntento;
        });
    }

    /** RF-34. */
    public Envio buscarPorId(int id) {
        return envioDao.findById(id).orElseThrow(() -> new EntidadNoEncontradaException("un envío", id));
    }

    /** RF-34. */
    public List<Envio> listarTodos() {
        return envioDao.findAll();
    }

    /**
     * RF-31 y RF-32. El plus lo calcula Envio con el precio de envío de la mercancía (que RF-26 deja fijo
     * mientras tenga envío) y el tipo del destinatario; el precio total es el guardado en el envío.
     */
    public DesgloseEnvio desglose(Envio envio) {
        BigDecimal precioBase = buscarMercancia(envio.getMercanciaId()).getPrecioEnvio();
        TipoCliente tipoDestinatario = buscarCliente(envio.getDestinatarioId()).getTipo();
        return new DesgloseEnvio(precioBase, envio.calcularPlus(precioBase, tipoDestinatario),
                envio.getPrecioTotal());
    }

    private Envio buscarPorId(int id, Connection conexion) {
        return envioDao.findById(id, conexion).orElseThrow(() -> new EntidadNoEncontradaException("un envío", id));
    }

    private Cliente buscarCliente(int id) {
        return clienteDao.findById(id).orElseThrow(() -> new EntidadNoEncontradaException("un cliente", id));
    }

    private Mercancia buscarMercancia(int id) {
        return mercanciaDao.findById(id).orElseThrow(() -> new EntidadNoEncontradaException("una mercancía", id));
    }

    private static void exigirClientesDistintos(int clienteOrigenId, int destinatarioId) {
        if (clienteOrigenId == destinatarioId) {
            throw new DatoInvalidoException("El cliente origen y el destinatario deben ser distintos.");
        }
    }

    private void exigirMercanciaSinEnvio(int mercanciaId) {
        if (mercanciaDao.tieneEnvio(mercanciaId)) {
            throw new MercanciaConEnvioException("La mercancía ya tiene un envío registrado.");
        }
    }

    /** Un delta cero no toca la BD. Si el cliente no existe, la excepción revierte la transacción. */
    private void sumarAlCliente(int clienteId, BigDecimal delta, Connection conexion) {
        if (delta.signum() == 0) {
            return;
        }
        if (!clienteDao.sumarTotalFacturado(clienteId, delta, conexion)) {
            throw new EntidadNoEncontradaException("un cliente", clienteId);
        }
    }
}
