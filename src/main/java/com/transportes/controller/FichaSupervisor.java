package com.transportes.controller;

import com.transportes.model.Supervisor;
import com.transportes.model.TipoVehiculo;
import com.transportes.model.Vehiculo;

import java.util.List;
import java.util.Objects;

/** Supervisor con los vehículos que tiene a cargo (RF-12) y su número de aviones (RF-10). */
public record FichaSupervisor(Supervisor supervisor, List<Vehiculo> vehiculos) {

    public FichaSupervisor {
        Objects.requireNonNull(supervisor, "supervisor");
        vehiculos = List.copyOf(vehiculos);
    }

    public int numeroAviones() {
        return (int) vehiculos.stream().filter(v -> v.getTipo() == TipoVehiculo.AVION).count();
    }
}
