package com.transportes.view;

import com.transportes.controller.ClienteController;

import java.util.List;
import java.util.Objects;

/** RF-14 a RF-18. Las opciones se añaden en la parte 3 de la Etapa 6. */
public class ClienteView extends VistaModulo {

    private final ClienteController clienteController;

    public ClienteView(Consola consola, ManejadorErrores manejador, ClienteController clienteController) {
        super(consola, manejador);
        this.clienteController = Objects.requireNonNull(clienteController, "clienteController");
    }

    @Override
    public String titulo() {
        return "Clientes";
    }

    @Override
    protected List<Opcion> opciones() {
        return List.of();
    }
}
