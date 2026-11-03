package com.transportes.view;

import com.transportes.controller.ClienteController;
import com.transportes.controller.HistorialCliente;
import com.transportes.controller.MercanciaController;
import com.transportes.model.Cliente;
import com.transportes.model.ClienteEmpresa;
import com.transportes.model.ClienteParticular;
import com.transportes.model.Envio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RF-14 a RF-18. El cliente se identifica por su código. La fecha de alta y el total facturado no se piden:
 * los pone el controlador (P-07, RF-17).
 */
public class ClienteView extends VistaModulo {

    private final ClienteController clienteController;
    private final MercanciaController mercanciaController;

    public ClienteView(Consola consola, ManejadorErrores manejador, ClienteController clienteController,
                       MercanciaController mercanciaController) {
        super(consola, manejador);
        this.clienteController = Objects.requireNonNull(clienteController, "clienteController");
        this.mercanciaController = Objects.requireNonNull(mercanciaController, "mercanciaController");
    }

    @Override
    public String titulo() {
        return "Clientes";
    }

    @Override
    protected List<Opcion> opciones() {
        return List.of(
                new Opcion("Registrar cliente particular", this::registrarParticular),
                new Opcion("Registrar cliente empresa", this::registrarEmpresa),
                new Opcion("Modificar cliente", this::modificar),
                new Opcion("Eliminar cliente", this::eliminar),
                new Opcion("Listar todos", this::listar),
                new Opcion("Consultar por código", this::consultar),
                new Opcion("Historial de envíos y total facturado", this::historial));
    }

    /** RF-14 y RF-15. */
    private void registrarParticular() {
        ClienteParticular cliente = clienteController.registrarParticular(
                consola.leerTextoNoVacio("Código de cliente"),
                consola.leerTextoNoVacio("NIF"),
                consola.leerTextoNoVacio("Nombre"),
                consola.leerTextoNoVacio("Dirección"));
        mostrarRegistrado(cliente);
    }

    /** RF-14 y RF-15. */
    private void registrarEmpresa() {
        ClienteEmpresa cliente = clienteController.registrarEmpresa(
                consola.leerTextoNoVacio("Código de cliente"),
                consola.leerTextoNoVacio("CIF"),
                consola.leerTextoNoVacio("Nombre"),
                consola.leerTextoNoVacio("Dirección"),
                consola.leerTextoNoVacio("Teléfono"));
        mostrarRegistrado(cliente);
    }

    /** RF-16. El código y el tipo no se piden; Enter conserva cada dato. */
    private void modificar() {
        Cliente actual = clienteController.buscarPorCodigo(consola.leerTextoNoVacio("Código de cliente"));
        consola.mostrar(resumen(actual));
        Cliente modificado = switch (actual.getTipo()) {
            case PARTICULAR -> new ClienteParticular(actual.getId(), actual.getCodigoCliente(),
                    actual.getFechaAlta(), actual.getTotalFacturado(),
                    consola.leerTextoOActual("NIF", ((ClienteParticular) actual).getNif()),
                    consola.leerTextoOActual("Nombre", actual.getNombre()),
                    consola.leerTextoOActual("Dirección", actual.getDireccion()));
            case EMPRESA -> new ClienteEmpresa(actual.getId(), actual.getCodigoCliente(),
                    actual.getFechaAlta(), actual.getTotalFacturado(),
                    consola.leerTextoOActual("CIF", ((ClienteEmpresa) actual).getCif()),
                    consola.leerTextoOActual("Nombre", actual.getNombre()),
                    consola.leerTextoOActual("Dirección", actual.getDireccion()),
                    consola.leerTextoOActual("Teléfono", ((ClienteEmpresa) actual).getTelefono()));
        };
        clienteController.modificar(modificado);
        consola.exito("Cliente " + actual.getCodigoCliente() + " modificado.");
    }

    /** RF-16: solo si no figura en ningún envío; si figura, el controlador lo rechaza. */
    private void eliminar() {
        Cliente cliente = clienteController.buscarPorCodigo(consola.leerTextoNoVacio("Código de cliente"));
        consola.mostrar(resumen(cliente));
        if (!consola.confirmar("¿Eliminar este cliente?")) {
            consola.mostrar("No se ha eliminado el cliente.");
            return;
        }
        clienteController.eliminar(cliente.getId());
        consola.exito("Cliente " + cliente.getCodigoCliente() + " eliminado.");
    }

    /** RF-16. */
    private void listar() {
        List<Cliente> clientes = clienteController.listarTodos();
        if (clientes.isEmpty()) {
            consola.mostrar("No hay clientes registrados.");
            return;
        }
        clientes.forEach(cliente -> consola.mostrar("- " + resumen(cliente)));
        consola.mostrar(clientes.size() + " cliente(s).");
    }

    /** RF-16. */
    private void consultar() {
        consola.mostrar(resumen(clienteController.buscarPorCodigo(consola.leerTextoNoVacio("Código de cliente"))));
    }

    private void historial() {
        mostrarHistorial(consola, clienteController, mercanciaController);
    }

    /**
     * RF-18 y RF-36: envíos encargados como cliente origen, por fecha. Cada código se busca una sola vez.
     * También lo usa ReportesView.
     */
    static void mostrarHistorial(Consola consola, ClienteController clienteController,
                                 MercanciaController mercanciaController) {
        Cliente cliente = clienteController.buscarPorCodigo(consola.leerTextoNoVacio("Código de cliente"));
        HistorialCliente historial = clienteController.historial(cliente.getId());
        consola.mostrar(resumen(historial.cliente()));
        if (historial.envios().isEmpty()) {
            consola.mostrar("  No ha encargado ningún envío.");
        } else {
            Map<Integer, String> clientes = new HashMap<>();
            Map<Integer, String> mercancias = new HashMap<>();
            for (Envio envio : historial.envios()) {
                consola.mostrar("  - " + Formato.fecha(envio.getFechaEnvio())
                        + " · destinatario " + clientes.computeIfAbsent(envio.getDestinatarioId(),
                                id -> clienteController.buscarPorId(id).getCodigoCliente())
                        + " · mercancía " + mercancias.computeIfAbsent(envio.getMercanciaId(),
                                id -> mercanciaController.buscarPorId(id).getCodigo())
                        + " · intentos fallidos " + envio.getIntentosFallidos()
                        + " · total " + Formato.dinero(envio.getPrecioTotal()));
            }
            consola.mostrar("  " + historial.envios().size() + " envío(s).");
        }
        consola.mostrar("  Total facturado: " + Formato.dinero(historial.totalFacturado()));
    }

    private void mostrarRegistrado(Cliente cliente) {
        consola.exito("Cliente " + cliente.getCodigoCliente() + " registrado con fecha de alta "
                + Formato.fecha(cliente.getFechaAlta()) + ".");
    }

    private static String resumen(Cliente cliente) {
        String datos = switch (cliente.getTipo()) {
            case PARTICULAR -> "NIF " + ((ClienteParticular) cliente).getNif() + " · " + cliente.getNombre()
                    + " · " + cliente.getDireccion();
            case EMPRESA -> "CIF " + ((ClienteEmpresa) cliente).getCif() + " · " + cliente.getNombre()
                    + " · " + cliente.getDireccion() + " · tel. " + ((ClienteEmpresa) cliente).getTelefono();
        };
        return cliente.getCodigoCliente() + " · " + cliente.getTipo() + " · " + datos
                + " · alta " + Formato.fecha(cliente.getFechaAlta())
                + " · total facturado " + Formato.dinero(cliente.getTotalFacturado());
    }
}
