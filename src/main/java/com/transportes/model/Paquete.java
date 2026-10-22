package com.transportes.model;

import java.math.BigDecimal;

public class Paquete extends Mercancia {

    private BigDecimal largo;
    private BigDecimal ancho;
    private BigDecimal alto;

    public Paquete(Integer id, String codigo, String descripcion, BigDecimal precioEnvio, boolean asegurada,
                   boolean largoRecorrido, int vehiculoId, Integer avionId, Integer vehiculoDestinoId,
                   BigDecimal largo, BigDecimal ancho, BigDecimal alto) {
        super(id, codigo, TipoMercancia.PAQUETE, descripcion, precioEnvio, asegurada, largoRecorrido,
                vehiculoId, avionId, vehiculoDestinoId);
        setLargo(largo);
        setAncho(ancho);
        setAlto(alto);
    }

    public BigDecimal getLargo() {
        return largo;
    }

    public void setLargo(BigDecimal largo) {
        this.largo = Validacion.positivo(largo, "largo");
    }

    public BigDecimal getAncho() {
        return ancho;
    }

    public void setAncho(BigDecimal ancho) {
        this.ancho = Validacion.positivo(ancho, "ancho");
    }

    public BigDecimal getAlto() {
        return alto;
    }

    public void setAlto(BigDecimal alto) {
        this.alto = Validacion.positivo(alto, "alto");
    }
}
