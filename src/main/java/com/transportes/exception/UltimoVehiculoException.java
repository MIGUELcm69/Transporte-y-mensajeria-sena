package com.transportes.exception;

/** Eliminar o reasignar el último vehículo de un supervisor lo dejaría sin vehículos (RF-13, P-01). */
public class UltimoVehiculoException extends DomainException {

    public UltimoVehiculoException(String mensaje) {
        super(mensaje);
    }
}
