package com.transportes.controller;

import com.transportes.model.Cliente;
import com.transportes.model.Envio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Cliente con los envíos que ha encargado como cliente origen, por fecha (RF-18, RF-36). */
public record HistorialCliente(Cliente cliente, List<Envio> envios) {

    public HistorialCliente {
        Objects.requireNonNull(cliente, "cliente");
        envios = List.copyOf(envios);
    }

    public BigDecimal totalFacturado() {
        return cliente.getTotalFacturado();
    }
}
