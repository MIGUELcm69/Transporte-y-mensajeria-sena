package com.transportes.view;

import com.transportes.controller.FichaSupervisor;
import com.transportes.controller.SupervisorController;
import com.transportes.model.Supervisor;
import com.transportes.model.Vehiculo;

import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

/** RF-09 a RF-13. El supervisor se identifica por NIF. */
public class SupervisorView extends VistaModulo {

    private final SupervisorController supervisorController;
    private final FormularioVehiculo formularioVehiculo;

    public SupervisorView(Consola consola, ManejadorErrores manejador, SupervisorController supervisorController) {
        super(consola, manejador);
        this.supervisorController = Objects.requireNonNull(supervisorController, "supervisorController");
        this.formularioVehiculo = new FormularioVehiculo(consola);
    }

    @Override
    public String titulo() {
        return "Supervisores";
    }

    @Override
    protected List<Opcion> opciones() {
        return List.of(
                new Opcion("Registrar supervisor con su primer vehículo", this::registrar),
                new Opcion("Modificar supervisor", this::modificar),
                new Opcion("Eliminar supervisor sin vehículos", this::eliminar),
                new Opcion("Eliminar supervisor traspasando sus vehículos a otro", this::eliminarTraspasando),
                new Opcion("Listar con sus vehículos y número de aviones", this::listar),
                new Opcion("Consultar por NIF", this::consultar));
    }

    /**
     * RF-09 y RF-13 (P-01). El supervisor se construye antes de pedir el vehículo, para que sus datos inválidos
     * se detecten sin tocar la BD. Ambos se guardan en una transacción: si falla, no queda ninguno.
     */
    private void registrar() {
        Supervisor supervisor = new Supervisor(null,
                consola.leerTextoNoVacio("NIF"),
                consola.leerTextoNoVacio("Nombre"),
                consola.leerTextoNoVacio("Dirección"),
                consola.leerTextoNoVacio("Teléfono"));
        consola.mostrar("Primer vehículo a su cargo (todo supervisor debe tener al menos uno).");
        IntFunction<Vehiculo> primerVehiculo = formularioVehiculo.pedirNuevo(formularioVehiculo.elegirTipo());
        FichaSupervisor ficha = supervisorController.registrar(supervisor, primerVehiculo);
        consola.exito("Supervisor " + supervisor.getNif() + " registrado con su primer vehículo.");
        mostrarFicha(ficha);
    }

    /** RF-11. Enter conserva cada dato. */
    private void modificar() {
        Supervisor actual = supervisorController.buscarPorNif(consola.leerTextoNoVacio("NIF del supervisor"));
        Supervisor modificado = new Supervisor(actual.getId(),
                consola.leerTextoOActual("NIF", actual.getNif()),
                consola.leerTextoOActual("Nombre", actual.getNombre()),
                consola.leerTextoOActual("Dirección", actual.getDireccion()),
                consola.leerTextoOActual("Teléfono", actual.getTelefono()));
        supervisorController.modificar(modificado);
        consola.exito("Supervisor " + modificado.getNif() + " modificado.");
    }

    /** RF-11: solo si no tiene vehículos a cargo; si los tiene, el controlador lo rechaza. */
    private void eliminar() {
        Supervisor supervisor = supervisorController.buscarPorNif(consola.leerTextoNoVacio("NIF del supervisor"));
        consola.mostrar(resumen(supervisor));
        if (!consola.confirmar("¿Eliminar este supervisor?")) {
            consola.mostrar("No se ha eliminado el supervisor.");
            return;
        }
        supervisorController.eliminar(supervisor.getId());
        consola.exito("Supervisor " + supervisor.getNif() + " eliminado.");
    }

    /** RF-11 y RF-13: traspaso de todos sus vehículos y borrado en una transacción. */
    private void eliminarTraspasando() {
        FichaSupervisor origen = supervisorController.consultar(
                supervisorController.buscarPorNif(consola.leerTextoNoVacio("NIF del supervisor a eliminar")).getId());
        Supervisor destino = supervisorController.buscarPorNif(
                consola.leerTextoNoVacio("NIF del supervisor que recibirá sus vehículos"));
        consola.mostrar("Se traspasarán " + origen.vehiculos().size() + " vehículo(s) de "
                + origen.supervisor().getNif() + " a " + destino.getNif() + " y se eliminará "
                + origen.supervisor().getNif() + ".");
        if (!consola.confirmar("¿Continuar?")) {
            consola.mostrar("No se ha eliminado el supervisor.");
            return;
        }
        int traspasados = supervisorController.eliminarTraspasandoVehiculos(origen.supervisor().getId(),
                destino.getId());
        consola.exito("Supervisor " + origen.supervisor().getNif() + " eliminado; " + traspasados
                + " vehículo(s) traspasado(s) a " + destino.getNif() + ".");
    }

    /** RF-10 y RF-12. */
    private void listar() {
        List<FichaSupervisor> fichas = supervisorController.listar();
        if (fichas.isEmpty()) {
            consola.mostrar("No hay supervisores registrados.");
            return;
        }
        fichas.forEach(this::mostrarFicha);
        consola.mostrar(fichas.size() + " supervisor(es).");
    }

    /** RF-10 y RF-12. */
    private void consultar() {
        Supervisor supervisor = supervisorController.buscarPorNif(consola.leerTextoNoVacio("NIF del supervisor"));
        mostrarFicha(supervisorController.consultar(supervisor.getId()));
    }

    private void mostrarFicha(FichaSupervisor ficha) {
        consola.mostrar(resumen(ficha.supervisor()));
        consola.mostrar("  Aviones a cargo: " + ficha.numeroAviones());
        consola.mostrar("  Vehículos a cargo (" + ficha.vehiculos().size() + "):");
        ficha.vehiculos().forEach(v -> consola.mostrar("    - " + FormularioVehiculo.resumen(v)));
    }

    private static String resumen(Supervisor supervisor) {
        return supervisor.getNif() + " · " + supervisor.getNombre() + " · " + supervisor.getDireccion()
                + " · tel. " + supervisor.getTelefono();
    }
}
