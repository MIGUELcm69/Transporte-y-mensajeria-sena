package com.transportes.exception;

/**
 * Base de las excepciones de dominio. Sus mensajes son aptos para mostrarse al usuario.
 */
public class DomainException extends RuntimeException {

    public DomainException(String mensaje) {
        super(mensaje);
    }

    public DomainException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
