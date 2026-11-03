package com.transportes.view;

import com.transportes.controller.MercanciaController;
import com.transportes.controller.VehiculoController;

import java.util.List;
import java.util.Objects;

/** RF-19 a RF-26. Las opciones se añaden en la parte 3 de la Etapa 6. */
public class MercanciaView extends VistaModulo {

    private final MercanciaController mercanciaController;
    private final VehiculoController vehiculoController;

    public MercanciaView(Consola consola, ManejadorErrores manejador, MercanciaController mercanciaController,
                         VehiculoController vehiculoController) {
        super(consola, manejador);
        this.mercanciaController = Objects.requireNonNull(mercanciaController, "mercanciaController");
        this.vehiculoController = Objects.requireNonNull(vehiculoController, "vehiculoController");
    }

    @Override
    public String titulo() {
        return "Mercancías";
    }

    @Override
    protected List<Opcion> opciones() {
        return List.of();
    }
}
