package com.transportes.view;

import com.transportes.controller.ClienteController;
import com.transportes.controller.DesgloseEnvio;
import com.transportes.controller.EnvioController;
import com.transportes.controller.MercanciaController;
import com.transportes.model.Envio;
import com.transportes.model.Mercancia;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * RF-27 a RF-34. Los clientes y la mercancía se identifican por código y el envío por su id. El plus y el
 * precio total los calcula el modelo; la vista solo muestra el desglose que devuelve el controlador.
 */
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
        return List.of(
                new Opcion("Registrar envío", this::registrar),
                new Opcion("Modificar envío", this::modificar),
                new Opcion("Eliminar envío", this::eliminar),
                new Opcion("Registrar intento de entrega fallido", this::registrarIntentoFallido),
                new Opcion("Listar todos", this::listar),
                new Opcion("Consultar por id", this::consultar));
    }

    /** RF-27 a RF-32. Sin mercancías libres no se pide nada; el controlador vuelve a comprobar que lo esté. */
    private void registrar() {
        List<Mercancia> libres = mercanciaController.listarSinEnvio();
        if (libres.isEmpty()) {
            consola.mostrar("No hay mercancías sin envío; registre antes una en Mercancías.");
            throw new OperacionCancelada();
        }
        int origenId = clienteController.buscarPorCodigo(consola.leerTextoNoVacio("Código del cliente origen"))
                .getId();
        int destinatarioId = clienteController.buscarPorCodigo(consola.leerTextoNoVacio("Código del destinatario"))
                .getId();
        mostrarMercancias("Mercancías sin envío:", libres);
        int mercanciaId = mercanciaController.buscarPorCodigo(consola.leerTextoNoVacio("Código de la mercancía"))
                .getId();
        LocalDate fecha = consola.leerFechaOHoy("Fecha de envío", LocalDate.now());
        Envio envio = envioController.crear(origenId, destinatarioId, mercanciaId, fecha);
        consola.exito("Envío " + envio.getId() + " registrado.");
        mostrarDesglose(envioController.desglose(envio));
    }

    /** RF-34. Enter conserva cada dato; el precio no se pide: lo recalcula el controlador con los intentos. */
    private void modificar() {
        Envio actual = envioController.buscarPorId(leerId());
        consola.mostrar(resumen(actual, this::codigoCliente, this::codigoMercancia));
        int origenId = elegirClienteOActual("Código del cliente origen", actual.getClienteOrigenId());
        int destinatarioId = elegirClienteOActual("Código del destinatario", actual.getDestinatarioId());
        mostrarMercancias("Otras mercancías sin envío:", mercanciaController.listarSinEnvio());
        String codigoMercancia = codigoMercancia(actual.getMercanciaId());
        String codigo = consola.leerTextoOActual("Código de la mercancía", codigoMercancia);
        int mercanciaId = codigo.equals(codigoMercancia)
                ? actual.getMercanciaId()
                : mercanciaController.buscarPorCodigo(codigo).getId();
        LocalDate fecha = consola.leerFechaOActual("Fecha de envío", actual.getFechaEnvio());
        Envio modificado = envioController.modificar(actual.getId(), origenId, destinatarioId, mercanciaId, fecha);
        consola.exito("Envío " + actual.getId() + " modificado.");
        mostrarDesglose(envioController.desglose(modificado));
    }

    /** RF-34. El controlador descuenta su precio total del cliente origen. */
    private void eliminar() {
        Envio envio = envioController.buscarPorId(leerId());
        consola.mostrar(resumen(envio, this::codigoCliente, this::codigoMercancia));
        if (!consola.confirmar("¿Eliminar este envío?")) {
            consola.mostrar("No se ha eliminado el envío.");
            return;
        }
        envioController.eliminar(envio.getId());
        consola.exito("Envío " + envio.getId() + " eliminado.");
    }

    /** RF-30 y RF-33: solo en envíos a particulares; el controlador rechaza los de empresas. */
    private void registrarIntentoFallido() {
        Envio conIntento = envioController.registrarIntentoFallido(leerId());
        consola.exito("Intento fallido registrado en el envío " + conIntento.getId() + "; lleva "
                + conIntento.getIntentosFallidos() + " intento(s) fallido(s).");
        DesgloseEnvio desglose = envioController.desglose(conIntento);
        consola.mostrar("  Nuevo plus de almacenaje: " + Formato.dinero(desglose.plus()));
        consola.mostrar("  Nuevo precio total: " + Formato.dinero(desglose.precioTotal()));
    }

    /** RF-34. Cada código se busca una sola vez por listado. */
    private void listar() {
        List<Envio> envios = envioController.listarTodos();
        if (envios.isEmpty()) {
            consola.mostrar("No hay envíos registrados.");
            return;
        }
        Map<Integer, String> clientes = new HashMap<>();
        Map<Integer, String> mercancias = new HashMap<>();
        for (Envio envio : envios) {
            consola.mostrar("- " + resumen(envio, id -> clientes.computeIfAbsent(id, this::codigoCliente),
                    id -> mercancias.computeIfAbsent(id, this::codigoMercancia)));
        }
        consola.mostrar(envios.size() + " envío(s).");
    }

    /** RF-34. */
    private void consultar() {
        Envio envio = envioController.buscarPorId(leerId());
        consola.mostrar(resumen(envio, this::codigoCliente, this::codigoMercancia));
        mostrarDesglose(envioController.desglose(envio));
    }

    private int leerId() {
        return consola.leerEntero("Id del envío", 1, Integer.MAX_VALUE);
    }

    private int elegirClienteOActual(String etiqueta, int actualId) {
        String codigoActual = codigoCliente(actualId);
        String codigo = consola.leerTextoOActual(etiqueta, codigoActual);
        return codigo.equals(codigoActual) ? actualId : clienteController.buscarPorCodigo(codigo).getId();
    }

    private void mostrarMercancias(String titulo, List<Mercancia> mercancias) {
        if (mercancias.isEmpty()) {
            consola.mostrar(titulo + " ninguna.");
            return;
        }
        consola.mostrar(titulo);
        mercancias.forEach(m -> consola.mostrar("  - " + FormularioMercancia.resumen(m)));
    }

    private void mostrarDesglose(DesgloseEnvio desglose) {
        consola.mostrar("  Precio base: " + Formato.dinero(desglose.precioBase()));
        consola.mostrar("  Plus de almacenaje: " + Formato.dinero(desglose.plus()));
        consola.mostrar("  Precio total: " + Formato.dinero(desglose.precioTotal()));
    }

    private String codigoCliente(int clienteId) {
        return clienteController.buscarPorId(clienteId).getCodigoCliente();
    }

    private String codigoMercancia(int mercanciaId) {
        return mercanciaController.buscarPorId(mercanciaId).getCodigo();
    }

    private static String resumen(Envio envio, IntFunction<String> codigoCliente,
                                  IntFunction<String> codigoMercancia) {
        return "Envío " + envio.getId() + " · " + Formato.fecha(envio.getFechaEnvio())
                + " · origen " + codigoCliente.apply(envio.getClienteOrigenId())
                + " · destinatario " + codigoCliente.apply(envio.getDestinatarioId())
                + " · mercancía " + codigoMercancia.apply(envio.getMercanciaId())
                + " · intentos fallidos " + envio.getIntentosFallidos()
                + " · total " + Formato.dinero(envio.getPrecioTotal());
    }
}
