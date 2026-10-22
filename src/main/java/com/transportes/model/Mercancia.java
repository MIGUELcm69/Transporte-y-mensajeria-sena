package com.transportes.model;

import com.transportes.exception.ReglaAsignacionException;

import java.math.BigDecimal;
import java.util.Objects;

public abstract class Mercancia {

    private Integer id;
    private final String codigo;
    private final TipoMercancia tipo;
    private String descripcion;
    private BigDecimal precioEnvio;
    private boolean asegurada;
    private boolean largoRecorrido;
    private int vehiculoId;
    private Integer avionId;
    private Integer vehiculoDestinoId;

    protected Mercancia(Integer id, String codigo, TipoMercancia tipo, String descripcion,
                        BigDecimal precioEnvio, boolean asegurada, boolean largoRecorrido,
                        int vehiculoId, Integer avionId, Integer vehiculoDestinoId) {
        this.id = Validacion.idOpcional(id, "id");
        this.codigo = Validacion.texto(codigo, "código", Validacion.LONGITUD_CODIGO);
        this.tipo = Validacion.obligatorio(tipo, "tipo");
        setDescripcion(descripcion);
        setPrecioEnvio(precioEnvio);
        setAsegurada(asegurada);
        asignarVehiculos(largoRecorrido, vehiculoId, avionId, vehiculoDestinoId);
    }

    public void asignarId(int id) {
        this.id = Validacion.idNuevo(this.id, id);
    }

    /**
     * Fija los tramos de la mercancía de forma conjunta para mantener RF-23/RF-24:
     * largo recorrido exige avión y vehículo de destino; si no es de largo recorrido, ninguno de los dos.
     * La comprobación del tipo de cada vehículo (RF-21/RF-25) requiere la BD y queda en el controlador.
     */
    public void asignarVehiculos(boolean largoRecorrido, int vehiculoId, Integer avionId, Integer vehiculoDestinoId) {
        int vehiculo = Validacion.id(vehiculoId, "vehículo");
        Integer avion = Validacion.idOpcional(avionId, "avión");
        Integer vehiculoDestino = Validacion.idOpcional(vehiculoDestinoId, "vehículo de destino");
        if (largoRecorrido && (avion == null || vehiculoDestino == null)) {
            throw new ReglaAsignacionException(
                    "Una mercancía de largo recorrido requiere avión y vehículo de destino.");
        }
        if (!largoRecorrido && (avion != null || vehiculoDestino != null)) {
            throw new ReglaAsignacionException(
                    "Solo las mercancías de largo recorrido pueden tener avión o vehículo de destino.");
        }
        this.largoRecorrido = largoRecorrido;
        this.vehiculoId = vehiculo;
        this.avionId = avion;
        this.vehiculoDestinoId = vehiculoDestino;
    }

    public Integer getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public TipoMercancia getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = Validacion.texto(descripcion, "descripción", Validacion.LONGITUD_DESCRIPCION);
    }

    public BigDecimal getPrecioEnvio() {
        return precioEnvio;
    }

    /** El bloqueo cuando ya existe un envío asociado (RF-26) lo aplica el controlador. */
    public void setPrecioEnvio(BigDecimal precioEnvio) {
        this.precioEnvio = Validacion.importe(precioEnvio, "precio de envío");
    }

    public boolean isAsegurada() {
        return asegurada;
    }

    public void setAsegurada(boolean asegurada) {
        this.asegurada = asegurada;
    }

    public boolean isLargoRecorrido() {
        return largoRecorrido;
    }

    public int getVehiculoId() {
        return vehiculoId;
    }

    public Integer getAvionId() {
        return avionId;
    }

    public Integer getVehiculoDestinoId() {
        return vehiculoDestinoId;
    }

    @Override
    public final boolean equals(Object o) {
        return this == o || (o instanceof Mercancia otra && codigo.equals(otra.codigo));
    }

    @Override
    public final int hashCode() {
        return Objects.hash(codigo);
    }
}
