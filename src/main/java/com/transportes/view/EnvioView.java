package com.transportes.view;

import com.transportes.controller.ClienteController;
import com.transportes.controller.EnvioController;
import com.transportes.controller.MercanciaController;

import java.util.List;
import java.util.Objects;

/** RF-27 a RF-34. Las opciones se añaden en la parte 4 de la Etapa 6. */
public class EnvioView extends VistaModulo {

    private final EnvioController envioController;
    private final ClienteController clienteController;
    private final MercanciaController mercanciaController;

    public EnvioView(Consola consola, ManejadorErrores manejador, EnvioController envioController,
                     ClienteController clienteController, MercanciaController mercanciaController) {
        super(consola, manejador);
        this.envioController = Objects.requireNonNull(envioController, "envioController");
        this.clienteController = Objects.requireNonNull(clienteController, "clienteController");
        this.mercanciaController = Objects.requireNonNull(mercanciaController, "mercanciaController");
    }

    @Override
    public String titulo() {
        return "Envíos";
    }

    @Override
    protected List<Opcion> opciones() {
        return List.of();
    }
}
