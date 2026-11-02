package com.transportes.model;

public enum TipoMercancia {
    SOBRE(TipoVehiculo.MOTO),
    PAQUETE(TipoVehiculo.FURGONETA);

    private final TipoVehiculo vehiculoTerrestre;

    TipoMercancia(TipoVehiculo vehiculoTerrestre) {
        this.vehiculoTerrestre = vehiculoTerrestre;
    }

    /**
     * Tipo de vehículo de los tramos terrestres: vehículo y vehículo de destino (RF-21, RF-24).
     * sp_validar_mercancia (sql/schema.sql) replica esta correspondencia: si cambia, hay que cambiar ambos.
     */
    public TipoVehiculo vehiculoTerrestre() {
        return vehiculoTerrestre;
    }
}
