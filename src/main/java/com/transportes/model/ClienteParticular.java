package com.transportes.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ClienteParticular extends Cliente {

    private String nif;

    public ClienteParticular(Integer id, String codigoCliente, LocalDate fechaAlta, BigDecimal totalFacturado,
                             String nif, String nombre, String direccion) {
        super(id, codigoCliente, TipoCliente.PARTICULAR, fechaAlta, totalFacturado, nombre, direccion);
        setNif(nif);
    }

    /** Cliente aún no guardado: sin id y con total facturado 0 (RF-14, RF-17). */
    public static ClienteParticular nuevo(String codigoCliente, LocalDate fechaAlta, String nif, String nombre,
                                          String direccion) {
        return new ClienteParticular(null, codigoCliente, fechaAlta, BigDecimal.ZERO, nif, nombre, direccion);
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = Validacion.texto(nif, "NIF", Validacion.LONGITUD_DOCUMENTO);
    }
}
