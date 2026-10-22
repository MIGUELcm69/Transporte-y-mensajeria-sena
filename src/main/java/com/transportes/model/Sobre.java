package com.transportes.model;

import java.math.BigDecimal;

public class Sobre extends Mercancia {

    private BigDecimal peso;

    public Sobre(Integer id, String codigo, String descripcion, BigDecimal precioEnvio, boolean asegurada,
                 boolean largoRecorrido, int vehiculoId, Integer avionId, Integer vehiculoDestinoId,
                 BigDecimal peso) {
        super(id, codigo, TipoMercancia.SOBRE, descripcion, precioEnvio, asegurada, largoRecorrido,
                vehiculoId, avionId, vehiculoDestinoId);
        setPeso(peso);
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = Validacion.positivo(peso, "peso");
    }
}
