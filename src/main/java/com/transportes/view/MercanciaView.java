package com.transportes.view;

import com.transportes.controller.MercanciaController;
import com.transportes.controller.VehiculoController;
import com.transportes.model.Mercancia;
import com.transportes.model.TipoVehiculo;
import com.transportes.model.Vehiculo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** RF-19 a RF-26. La mercancía y sus vehículos se identifican por código. */
public class MercanciaView extends VistaModulo {

    private final MercanciaController mercanciaController;
    private final VehiculoController vehiculoController;
    private final FormularioMercancia formulario;

    public MercanciaView(Consola consola, ManejadorErrores manejador, MercanciaController mercanciaController,
                         VehiculoController vehiculoController) {
        super(consola, manejador);
        this.mercanciaController = Objects.requireNonNull(mercanciaController, "mercanciaController");
        this.vehiculoController = Objects.requireNonNull(vehiculoController, "vehiculoController");
        this.formulario = new FormularioMercancia(consola, this::elegirVehiculo);
    }

    @Override
    public String titulo() {
        return "Mercancías";
    }

    @Override
    protected List<Opcion> opciones() {
        return List.of(
                new Opcion("Registrar mercancía", this::registrar),
                new Opcion("Modificar mercancía", this::modificar),
                new Opcion("Eliminar mercancía", this::eliminar),
                new Opcion("Listar todas", this::listar),
                new Opcion("Consultar por código", this::consultar));
    }

    /** RF-19 a RF-25. El controlador comprueba el tipo de cada vehículo elegido. */
    private void registrar() {
        Mercancia mercancia = formulario.pedirNueva(formulario.elegirTipo());
        mercanciaController.registrar(mercancia);
        consola.exito("Mercancía " + mercancia.getCodigo() + " registrada.");
    }

    /** RF-26. El código y el tipo no se modifican; con un envío asociado el precio de envío no se pregunta. */
    private void modificar() {
        Mercancia actual = mercanciaController.buscarPorCodigo(consola.leerTextoNoVacio("Código de la mercancía"));
        mostrarDetalle(actual);
        Optional<String> bloqueoPrecio = mercanciaController.bloqueoPrecioEnvio(actual.getId());
        bloqueoPrecio.ifPresent(motivo -> consola.mostrar(motivo + " Se conserva "
                + Formato.dinero(actual.getPrecioEnvio()) + "."));
        mercanciaController.modificar(formulario.pedirModificada(actual, bloqueoPrecio.isEmpty()));
        consola.exito("Mercancía " + actual.getCodigo() + " modificada.");
    }

    /** RF-26: solo sin envío asociado; si lo tiene, el controlador lo rechaza. */
    private void eliminar() {
        Mercancia mercancia = mercanciaController.buscarPorCodigo(consola.leerTextoNoVacio("Código de la mercancía"));
        mostrarDetalle(mercancia);
        if (!consola.confirmar("¿Eliminar esta mercancía?")) {
            consola.mostrar("No se ha eliminado la mercancía.");
            return;
        }
        mercanciaController.eliminar(mercancia.getId());
        consola.exito("Mercancía " + mercancia.getCodigo() + " eliminada.");
    }

    /** RF-26. Cada vehículo se busca una sola vez por listado. */
    private void listar() {
        List<Mercancia> mercancias = mercanciaController.listarTodos();
        if (mercancias.isEmpty()) {
            consola.mostrar("No hay mercancías registradas.");
            return;
        }
        Map<Integer, String> codigos = new HashMap<>();
        for (Mercancia mercancia : mercancias) {
            consola.mostrar("- " + FormularioMercancia.resumen(mercancia) + " · "
                    + FormularioMercancia.tramos(mercancia, id -> codigos.computeIfAbsent(id, this::codigoVehiculo)));
        }
        consola.mostrar(mercancias.size() + " mercancía(s).");
    }

    /** RF-26. */
    private void consultar() {
        mostrarDetalle(mercanciaController.buscarPorCodigo(consola.leerTextoNoVacio("Código de la mercancía")));
    }

    private void mostrarDetalle(Mercancia mercancia) {
        consola.mostrar(FormularioMercancia.resumen(mercancia));
        consola.mostrar("  " + FormularioMercancia.tramos(mercancia, this::codigoVehiculo));
    }

    /**
     * Lista los vehículos del tipo pedido antes de preguntar el código. El tipo lo vuelve a comprobar el
     * controlador al guardar; aquí solo se traduce el código a id.
     */
    private int elegirVehiculo(String etiqueta, TipoVehiculo tipo, Integer actualId) {
        List<Vehiculo> disponibles = vehiculoController.listarPorTipo(tipo);
        if (disponibles.isEmpty()) {
            consola.mostrar("No hay vehículos de tipo " + tipo + " registrados; regístrelo antes en Vehículos.");
            throw new OperacionCancelada();
        }
        consola.mostrar(etiqueta + " · vehículos de tipo " + tipo + ":");
        disponibles.forEach(v -> consola.mostrar("  - " + FormularioVehiculo.resumen(v)));
        if (actualId == null) {
            return vehiculoController.buscarPorCodigo(consola.leerTextoNoVacio("Código del " + etiqueta.toLowerCase()))
                    .getId();
        }
        String codigoActual = codigoVehiculo(actualId);
        String codigo = consola.leerTextoOActual("Código del " + etiqueta.toLowerCase(), codigoActual);
        return codigo.equals(codigoActual) ? actualId : vehiculoController.buscarPorCodigo(codigo).getId();
    }

    private String codigoVehiculo(int vehiculoId) {
        return vehiculoController.buscarPorId(vehiculoId).getCodigo();
    }
}
