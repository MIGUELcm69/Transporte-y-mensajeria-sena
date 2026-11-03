package com.transportes.view;

import com.transportes.controller.SupervisorController;
import com.transportes.controller.VehiculoController;
import com.transportes.model.Supervisor;
import com.transportes.model.Vehiculo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** RF-01 a RF-08. El supervisor se identifica por NIF y el vehículo por código. */
public class VehiculoView extends VistaModulo {

    private final VehiculoController vehiculoController;
    private final SupervisorController supervisorController;
    private final FormularioVehiculo formulario;

    public VehiculoView(Consola consola, ManejadorErrores manejador, VehiculoController vehiculoController,
                        SupervisorController supervisorController) {
        super(consola, manejador);
        this.vehiculoController = Objects.requireNonNull(vehiculoController, "vehiculoController");
        this.supervisorController = Objects.requireNonNull(supervisorController, "supervisorController");
        this.formulario = new FormularioVehiculo(consola);
    }

    @Override
    public String titulo() {
        return "Vehículos";
    }

    @Override
    protected List<Opcion> opciones() {
        return List.of(
                new Opcion("Registrar vehículo para un supervisor existente", this::registrar),
                new Opcion("Modificar vehículo", this::modificar),
                new Opcion("Eliminar vehículo", this::eliminar),
                new Opcion("Listar todos", this::listarTodos),
                new Opcion("Listar por tipo", this::listarPorTipo),
                new Opcion("Consultar por código", this::consultar));
    }

    /** RF-01 a RF-04 y RF-08. Un supervisor nuevo se registra desde Supervisores, con su primer vehículo. */
    private void registrar() {
        Supervisor supervisor = supervisorController.buscarPorNif(consola.leerTextoNoVacio("NIF del supervisor"));
        Vehiculo vehiculo = formulario.pedirNuevo(formulario.elegirTipo()).apply(supervisor.getId());
        vehiculoController.registrar(vehiculo);
        consola.exito("Vehículo " + vehiculo.getCodigo() + " registrado a cargo de " + supervisor.getNombre() + ".");
    }

    /** RF-05 y RF-08. El código y el tipo no se modifican; cambiar de supervisor respeta RF-13 en el controlador. */
    private void modificar() {
        Vehiculo actual = vehiculoController.buscarPorCodigo(consola.leerTextoNoVacio("Código del vehículo"));
        Supervisor supervisorActual = supervisorController.buscarPorId(actual.getSupervisorId());
        consola.mostrar(FormularioVehiculo.resumen(actual) + " · supervisor " + supervisorActual.getNif());
        String nif = consola.leerTextoOActual("NIF del supervisor", supervisorActual.getNif());
        int supervisorId = nif.equals(supervisorActual.getNif())
                ? supervisorActual.getId()
                : supervisorController.buscarPorNif(nif).getId();
        vehiculoController.modificar(formulario.pedirModificado(actual, supervisorId));
        consola.exito("Vehículo " + actual.getCodigo() + " modificado.");
    }

    /** RF-06 y RF-13. */
    private void eliminar() {
        Vehiculo vehiculo = vehiculoController.buscarPorCodigo(consola.leerTextoNoVacio("Código del vehículo"));
        consola.mostrar(FormularioVehiculo.resumen(vehiculo));
        if (!consola.confirmar("¿Eliminar este vehículo?")) {
            consola.mostrar("No se ha eliminado el vehículo.");
            return;
        }
        vehiculoController.eliminar(vehiculo.getId());
        consola.exito("Vehículo " + vehiculo.getCodigo() + " eliminado.");
    }

    /** RF-07. */
    private void listarTodos() {
        mostrarLista(vehiculoController.listarTodos(), "No hay vehículos registrados.");
    }

    /** RF-07. */
    private void listarPorTipo() {
        mostrarLista(vehiculoController.listarPorTipo(formulario.elegirTipo()), "No hay vehículos de ese tipo.");
    }

    /** RF-07. */
    private void consultar() {
        Vehiculo vehiculo = vehiculoController.buscarPorCodigo(consola.leerTextoNoVacio("Código del vehículo"));
        Supervisor supervisor = supervisorController.buscarPorId(vehiculo.getSupervisorId());
        consola.mostrar(FormularioVehiculo.resumen(vehiculo));
        consola.mostrar("  Supervisor: " + supervisor.getNif() + " · " + supervisor.getNombre());
    }

    /** Cada supervisor se busca una sola vez por listado. */
    private void mostrarLista(List<Vehiculo> vehiculos, String siVacia) {
        if (vehiculos.isEmpty()) {
            consola.mostrar(siVacia);
            return;
        }
        Map<Integer, Supervisor> supervisores = new HashMap<>();
        for (Vehiculo vehiculo : vehiculos) {
            Supervisor supervisor = supervisores.computeIfAbsent(vehiculo.getSupervisorId(),
                    supervisorController::buscarPorId);
            consola.mostrar("- " + FormularioVehiculo.resumen(vehiculo) + " · supervisor " + supervisor.getNif());
        }
        consola.mostrar(vehiculos.size() + " vehículo(s).");
    }
}
