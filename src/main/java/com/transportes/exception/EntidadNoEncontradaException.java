package com.transportes.exception;

public class EntidadNoEncontradaException extends DomainException {

    public EntidadNoEncontradaException(String entidad, Object identificador) {
        super("No existe " + entidad + " con identificador '" + identificador + "'.");
    }
}
