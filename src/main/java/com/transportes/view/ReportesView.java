package com.transportes.view;

import com.transportes.controller.ClienteController;
import com.transportes.controller.MercanciaController;
import com.transportes.controller.VehiculoController;

import java.util.List;
import java.util.Objects;

/** RF-35 a RF-37. Las opciones se añaden en la parte 4 de la Etapa 6. */
public class ReportesView extends VistaModulo {

    private final MercanciaController mercanciaController;
    private final VehiculoController vehiculoController;
    private final ClienteController clienteController;

    public ReportesView(Consola consola, ManejadorErrores manejador, MercanciaController mercanciaController,
                        VehiculoController vehiculoController, ClienteController clienteController) {
        super(consola, manejador);
        this.mercanciaController = Objects.requireNonNull(mercanciaController, "mercanciaController");
        this.vehiculoController = Objects.requireNonNull(vehiculoController, "vehiculoController");
        this.clienteController = Objects.requireNonNull(clienteController, "clienteController");
    }

    @Override
    public String titulo() {
        return "Reportes";
    }

    @Override
    protected List<Opcion> opciones() {
        return List.of();
    }
}
