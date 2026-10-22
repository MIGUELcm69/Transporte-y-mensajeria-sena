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

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = Validacion.texto(nif, "NIF", Validacion.LONGITUD_DOCUMENTO);
    }
}
