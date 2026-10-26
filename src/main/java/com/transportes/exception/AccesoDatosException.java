package com.transportes.exception;

/**
 * Fallo de infraestructura al acceder a la BD (conexión, bloqueos, errores no previstos).
 * No es de dominio: el mensaje es genérico y el detalle técnico queda en el log.
 */
public class AccesoDatosException extends RuntimeException {

    public AccesoDatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
