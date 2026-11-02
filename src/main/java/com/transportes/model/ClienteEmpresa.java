package com.transportes.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ClienteEmpresa extends Cliente {

    private String cif;
    private String telefono;

    public ClienteEmpresa(Integer id, String codigoCliente, LocalDate fechaAlta, BigDecimal totalFacturado,
                          String cif, String nombre, String direccion, String telefono) {
        super(id, codigoCliente, TipoCliente.EMPRESA, fechaAlta, totalFacturado, nombre, direccion);
        setCif(cif);
        setTelefono(telefono);
    }

    /** Cliente aún no guardado: sin id y con total facturado 0 (RF-14, RF-17). */
    public static ClienteEmpresa nuevo(String codigoCliente, LocalDate fechaAlta, String cif, String nombre,
                                       String direccion, String telefono) {
        return new ClienteEmpresa(null, codigoCliente, fechaAlta, BigDecimal.ZERO, cif, nombre, direccion, telefono);
    }

    public String getCif() {
        return cif;
    }

    public void setCif(String cif) {
        this.cif = Validacion.texto(cif, "CIF", Validacion.LONGITUD_DOCUMENTO);
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = Validacion.texto(telefono, "teléfono", Validacion.LONGITUD_TELEFONO);
    }
}
