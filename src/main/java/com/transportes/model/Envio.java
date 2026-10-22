package com.transportes.model;

import com.transportes.exception.DatoInvalidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Envío inmutable. Solo guarda IDs; el precio de la mercancía y el tipo del destinatario
 * se reciben como parámetros para calcular plus y precio total (RF-31/RF-32).
 */
public class Envio {

    private static final BigDecimal TASA_BASE_PLUS = new BigDecimal("0.04");
    private static final BigDecimal TASA_POR_INTENTO = new BigDecimal("0.01");

    private Integer id;
    private final int clienteOrigenId;
    private final int destinatarioId;
    private final int mercanciaId;
    private final LocalDate fechaEnvio;
    private final int intentosFallidos;
    private final BigDecimal precioTotal;

    public Envio(Integer id, int clienteOrigenId, int destinatarioId, int mercanciaId,
                 LocalDate fechaEnvio, int intentosFallidos, BigDecimal precioTotal) {
        this.id = Validacion.idOpcional(id, "id");
        this.clienteOrigenId = Validacion.id(clienteOrigenId, "cliente origen");
        this.destinatarioId = Validacion.id(destinatarioId, "destinatario");
        if (clienteOrigenId == destinatarioId) {
            throw new DatoInvalidoException("El cliente origen y el destinatario deben ser distintos.");
        }
        this.mercanciaId = Validacion.id(mercanciaId, "mercancía");
        this.fechaEnvio = Validacion.obligatorio(fechaEnvio, "fecha de envío");
        this.intentosFallidos = Validacion.noNegativo(intentosFallidos, "intentos fallidos");
        this.precioTotal = Validacion.importe(precioTotal, "precio total");
    }

    /** Crea un envío nuevo, sin intentos fallidos, con el precio total ya calculado. */
    public static Envio nuevo(int clienteOrigenId, int destinatarioId, int mercanciaId, LocalDate fechaEnvio,
                              BigDecimal precioEnvio, TipoCliente tipoDestinatario) {
        BigDecimal precioTotal = precioTotal(precioEnvio, tipoDestinatario, 0);
        return new Envio(null, clienteOrigenId, destinatarioId, mercanciaId, fechaEnvio, 0, precioTotal);
    }

    /** Devuelve una copia con un intento fallido más y el precio total recalculado (RF-33). */
    public Envio conIntentoFallido(BigDecimal precioEnvio, TipoCliente tipoDestinatario) {
        if (Validacion.obligatorio(tipoDestinatario, "tipo de destinatario") != TipoCliente.PARTICULAR) {
            throw new DatoInvalidoException("Solo se registran intentos fallidos en envíos a particulares.");
        }
        int intentos = intentosFallidos + 1;
        return new Envio(id, clienteOrigenId, destinatarioId, mercanciaId, fechaEnvio, intentos,
                precioTotal(precioEnvio, tipoDestinatario, intentos));
    }

    public BigDecimal calcularPlus(BigDecimal precioEnvio, TipoCliente tipoDestinatario) {
        return plus(precioEnvio, tipoDestinatario, intentosFallidos);
    }

    public BigDecimal calcularPrecioTotal(BigDecimal precioEnvio, TipoCliente tipoDestinatario) {
        return precioTotal(precioEnvio, tipoDestinatario, intentosFallidos);
    }

    private static BigDecimal plus(BigDecimal precioEnvio, TipoCliente tipoDestinatario, int intentos) {
        BigDecimal precio = Validacion.importe(precioEnvio, "precio de envío");
        if (Validacion.obligatorio(tipoDestinatario, "tipo de destinatario") != TipoCliente.PARTICULAR) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal factor = TASA_BASE_PLUS.add(TASA_POR_INTENTO.multiply(BigDecimal.valueOf(intentos)));
        return precio.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal precioTotal(BigDecimal precioEnvio, TipoCliente tipoDestinatario, int intentos) {
        BigDecimal precio = Validacion.importe(precioEnvio, "precio de envío");
        return precio.add(plus(precio, tipoDestinatario, intentos)).setScale(2, RoundingMode.HALF_UP);
    }

    public void asignarId(int id) {
        this.id = Validacion.idNuevo(this.id, id);
    }

    public Integer getId() {
        return id;
    }

    public int getClienteOrigenId() {
        return clienteOrigenId;
    }

    public int getDestinatarioId() {
        return destinatarioId;
    }

    public int getMercanciaId() {
        return mercanciaId;
    }

    public LocalDate getFechaEnvio() {
        return fechaEnvio;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public BigDecimal getPrecioTotal() {
        return precioTotal;
    }

    /** Igualdad por id; sin id (no persistido) solo es igual a sí mismo. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof Envio otro && id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }
}
