package com.transportes.view;

import com.transportes.controller.ClienteController;
import com.transportes.controller.MercanciaController;
import com.transportes.controller.VehiculoController;
import com.transportes.model.Mercancia;
import com.transportes.model.Vehiculo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** RF-35 a RF-37. RF-36 se conecta en la parte 4 de la Etapa 6. */
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
        return List.of(
                new Opcion("Mercancías asignadas a cada vehículo", this::mercanciasPorVehiculo),
                new Opcion("Mercancías de largo recorrido con su avión", this::largoRecorrido));
    }

    /** RF-35 (y RF-22: un vehículo puede no tener ninguna). */
    private void mercanciasPorVehiculo() {
        List<Vehiculo> vehiculos = vehiculoController.listarTodos();
        if (vehiculos.isEmpty()) {
            consola.mostrar("No hay vehículos registrados.");
            return;
        }
        Map<Integer, String> codigos = new HashMap<>();
        vehiculos.forEach(v -> codigos.put(v.getId(), v.getCodigo()));
        for (Vehiculo vehiculo : vehiculos) {
            consola.mostrar(FormularioVehiculo.resumen(vehiculo));
            List<Mercancia> mercancias = mercanciaController.listarPorVehiculo(vehiculo.getId());
            if (mercancias.isEmpty()) {
                consola.mostrar("    (sin mercancías)");
            }
            mercancias.forEach(m -> consola.mostrar("    - " + FormularioMercancia.resumen(m) + " · "
                    + FormularioMercancia.tramos(m, codigos::get)));
        }
    }

    /** RF-37. */
    private void largoRecorrido() {
        List<Mercancia> mercancias = mercanciaController.listarLargoRecorrido();
        if (mercancias.isEmpty()) {
            consola.mostrar("No hay mercancías de largo recorrido.");
            return;
        }
        Map<Integer, Vehiculo> vehiculos = new HashMap<>();
        for (Mercancia mercancia : mercancias) {
            consola.mostrar("- " + FormularioMercancia.resumen(mercancia));
            consola.mostrar("    Avión: " + FormularioVehiculo.resumen(
                    vehiculos.computeIfAbsent(mercancia.getAvionId(), vehiculoController::buscarPorId)));
            consola.mostrar("    " + FormularioMercancia.tramos(mercancia,
                    id -> vehiculos.computeIfAbsent(id, vehiculoController::buscarPorId).getCodigo()));
        }
        consola.mostrar(mercancias.size() + " mercancía(s) de largo recorrido.");
    }
}
