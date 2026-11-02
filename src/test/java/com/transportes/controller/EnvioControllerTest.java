package com.transportes.controller;

import com.transportes.exception.DatoInvalidoException;
import com.transportes.exception.EntidadNoEncontradaException;
import com.transportes.exception.MercanciaConEnvioException;
import com.transportes.model.Envio;
import com.transportes.model.Mercancia;
import com.transportes.model.Sobre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.transportes.controller.ClienteDaoEnMemoria.empresa;
import static com.transportes.controller.ClienteDaoEnMemoria.particular;
import static com.transportes.controller.MercanciaDaoEnMemoria.sobre;
import static com.transportes.controller.VehiculoDaoEnMemoria.moto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Precio de envío de las mercancías sembradas: 20.00 (MercanciaDaoEnMemoria.PRECIO_ENVIO) y 50.00. */
class EnvioControllerTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 3, 1);
    private static final int SUPERVISOR = 1;

    private final EnvioDaoEnMemoria envios = new EnvioDaoEnMemoria();
    private final ClienteDaoEnMemoria clientes = new ClienteDaoEnMemoria(envios);
    private final MercanciaDaoEnMemoria mercancias = new MercanciaDaoEnMemoria(envios);
    private final VehiculoDaoEnMemoria vehiculos = new VehiculoDaoEnMemoria();
    private final TransaccionesEnMemoria transacciones = new TransaccionesEnMemoria(envios, clientes);
    private final EnvioController controller = new EnvioController(envios, clientes, mercancias, transacciones);

    private int ana;
    private int luis;
    private int acme;
    private int sobreId;
    private int sobreCaroId;

    @BeforeEach
    void sembrar() {
        ana = clientes.sembrar(particular("C-1", "11111111A")).getId();
        luis = clientes.sembrar(particular("C-2", "22222222B")).getId();
        acme = clientes.sembrar(empresa("C-3", "B12345678")).getId();
        int motoId = vehiculos.sembrar(moto("M-1", SUPERVISOR)).getId();
        sobreId = mercancias.sembrar(sobre("S-1", motoId)).getId();
        Sobre caro = sobre("S-2", motoId);
        caro.setPrecioEnvio(new BigDecimal("50.00"));
        sobreCaroId = mercancias.sembrar(caro).getId();
    }

    private void assertTotal(String esperado, int clienteId) {
        BigDecimal total = clientes.findById(clienteId).orElseThrow().getTotalFacturado();
        assertEquals(0, new BigDecimal(esperado).compareTo(total), "total facturado: " + total);
    }

    @Test
    void crearAParticularSumaElPrecioConPlusAlClienteOrigen() {
        Envio envio = controller.crear(ana, luis, sobreId, FECHA);

        // 20.00 + 20.00 × 0.04
        assertEquals(new BigDecimal("20.80"), envio.getPrecioTotal());
        assertTotal("20.80", ana);
        assertTotal("0", luis);
        assertEquals(1, transacciones.confirmadas);
        assertEquals(0, envios.escriturasSinTransaccion);
    }

    @Test
    void crearAEmpresaNoAplicaPlus() {
        Envio envio = controller.crear(ana, acme, sobreId, FECHA);

        assertEquals(new BigDecimal("20.00"), envio.getPrecioTotal());
        assertTotal("20.00", ana);
    }

    @Test
    void crearRechazaOrigenIgualAlDestinatario() {
        assertThrows(DatoInvalidoException.class, () -> controller.crear(ana, ana, sobreId, FECHA));

        assertTrue(envios.findAll().isEmpty());
        assertEquals(0, transacciones.confirmadas + transacciones.revertidas);
    }

    @Test
    void crearRechazaDestinatarioInexistente() {
        assertThrows(EntidadNoEncontradaException.class, () -> controller.crear(ana, 99, sobreId, FECHA));

        assertTrue(envios.findAll().isEmpty());
        assertTotal("0", ana);
    }

    @Test
    void crearRechazaMercanciaYaAsociadaAOtroEnvio() {
        controller.crear(ana, luis, sobreId, FECHA);

        assertThrows(MercanciaConEnvioException.class, () -> controller.crear(luis, acme, sobreId, FECHA));

        assertEquals(1, envios.findAll().size());
        assertTotal("0", luis);
    }

    @Test
    void modificarConOtroClienteOrigenRestaAlAnteriorYSumaAlNuevo() {
        Envio envio = controller.crear(ana, luis, sobreId, FECHA);

        controller.modificar(envio.getId(), acme, luis, sobreId, FECHA);

        assertTotal("0", ana);
        assertTotal("20.80", acme);
        assertEquals(acme, controller.buscarPorId(envio.getId()).getClienteOrigenId());
        assertEquals(1, envios.lecturasEnTransaccion);
        assertEquals(0, envios.escriturasSinTransaccion);
    }

    @Test
    void modificarConOtraMercanciaSumaLaDiferenciaDelPrecioTotal() {
        Envio envio = controller.crear(ana, luis, sobreId, FECHA);

        Envio modificado = controller.modificar(envio.getId(), ana, luis, sobreCaroId, FECHA);

        // 50.00 + 50.00 × 0.04
        assertEquals(new BigDecimal("52.00"), modificado.getPrecioTotal());
        assertTotal("52.00", ana);
        assertFalse(mercancias.tieneEnvio(sobreId));
    }

    @Test
    void modificarConservaLosIntentosYRecalculaElPlusConEllos() {
        Envio envio = controller.crear(ana, luis, sobreId, FECHA);
        controller.registrarIntentoFallido(envio.getId());

        Envio modificado = controller.modificar(envio.getId(), ana, luis, sobreCaroId, FECHA);

        // 50.00 + 50.00 × 0.05
        assertEquals(1, modificado.getIntentosFallidos());
        assertEquals(new BigDecimal("52.50"), modificado.getPrecioTotal());
        assertTotal("52.50", ana);
    }

    @Test
    void modificarRechazaDestinatarioEmpresaSiYaHayIntentosFallidos() {
        Envio envio = controller.crear(ana, luis, sobreId, FECHA);
        controller.registrarIntentoFallido(envio.getId());

        DatoInvalidoException error = assertThrows(DatoInvalidoException.class,
                () -> controller.modificar(envio.getId(), ana, acme, sobreId, FECHA));

        assertTrue(error.getMessage().contains("intento"), error.getMessage());
        assertEquals(luis, controller.buscarPorId(envio.getId()).getDestinatarioId());
        assertTotal("21.00", ana);
        assertEquals(1, transacciones.revertidas);
    }

    @Test
    void modificarSinCambioDePrecioNoLlamaASumarTotalFacturado() {
        Envio envio = controller.crear(ana, luis, sobreId, FECHA);
        int sumasAntes = clientes.sumasDeTotal;

        controller.modificar(envio.getId(), ana, luis, sobreId, FECHA.plusDays(2));

        assertEquals(sumasAntes, clientes.sumasDeTotal);
        assertEquals(FECHA.plusDays(2), controller.buscarPorId(envio.getId()).getFechaEnvio());
        assertTotal("20.80", ana);
    }

    @Test
    void modificarRechazaMercanciaQueYaTieneOtroEnvio() {
        Envio envio = controller.crear(ana, luis, sobreId, FECHA);
        controller.crear(luis, ana, sobreCaroId, FECHA);

        assertThrows(MercanciaConEnvioException.class,
                () -> controller.modificar(envio.getId(), ana, luis, sobreCaroId, FECHA));

        assertEquals(sobreId, controller.buscarPorId(envio.getId()).getMercanciaId());
        assertTotal("20.80", ana);
    }

    @Test
    void modificarEnvioInexistente() {
        assertThrows(EntidadNoEncontradaException.class, () -> controller.modificar(99, ana, luis, sobreId, FECHA));
    }

    @Test
    void eliminarRestaElPrecioTotalAlClienteOrigen() {
        Envio envio = controller.crear(ana, luis, sobreId, FECHA);

        controller.eliminar(envio.getId());

        assertTrue(envios.findAll().isEmpty());
        assertTotal("0", ana);
        assertEquals(1, envios.lecturasEnTransaccion);
        assertEquals(0, envios.escriturasSinTransaccion);
    }

    @Test
    void trasEliminarUnEnvioLaMercanciaPuedeAsignarseAOtro() {
        Envio envio = controller.crear(ana, luis, sobreId, FECHA);
        controller.eliminar(envio.getId());

        Envio otro = controller.crear(luis, acme, sobreId, FECHA);

        assertEquals(sobreId, otro.getMercanciaId());
        assertTotal("0", ana);
        assertTotal("20.00", luis);
    }

    @Test
    void eliminarRevierteSiNoSePuedeAjustarElTotalDelCliente() {
        Envio envio = controller.crear(ana, luis, sobreId, FECHA);
        clientes.delete(ana);

        assertThrows(EntidadNoEncontradaException.class, () -> controller.eliminar(envio.getId()));

        assertTrue(envios.findById(envio.getId()).isPresent());
        assertEquals(1, transacciones.revertidas);
    }

    @Test
    void eliminarEnvioInexistente() {
        assertThrows(EntidadNoEncontradaException.class, () -> controller.eliminar(99));
    }

    @Test
    void registrarIntentoFallidoRecalculaElPlusYSumaLaDiferencia() {
        Envio envio = controller.crear(ana, luis, sobreId, FECHA);

        Envio conIntento = controller.registrarIntentoFallido(envio.getId());

        // 20.00 + 20.00 × 0.05
        assertEquals(1, conIntento.getIntentosFallidos());
        assertEquals(new BigDecimal("21.00"), conIntento.getPrecioTotal());
        assertTotal("21.00", ana);
        assertEquals(0, envios.escriturasSinTransaccion);
    }

    @Test
    void dosIntentosFallidosSeguidosDejanElTotalFacturadoCoherente() {
        Envio envio = controller.crear(ana, luis, sobreId, FECHA);

        controller.registrarIntentoFallido(envio.getId());
        Envio conDosIntentos = controller.registrarIntentoFallido(envio.getId());

        // 20.00 + 20.00 × 0.06
        assertEquals(2, conDosIntentos.getIntentosFallidos());
        assertEquals(new BigDecimal("21.20"), controller.buscarPorId(envio.getId()).getPrecioTotal());
        assertTotal("21.20", ana);
    }

    @Test
    void registrarIntentoFallidoRechazaEnvioAEmpresa() {
        Envio envio = controller.crear(ana, acme, sobreId, FECHA);

        assertThrows(DatoInvalidoException.class, () -> controller.registrarIntentoFallido(envio.getId()));

        assertEquals(0, controller.buscarPorId(envio.getId()).getIntentosFallidos());
        assertTotal("20.00", ana);
    }

    @Test
    void conEnvioNoSeCambiaElPrecioEnvioYElIntentoFallidoUsaElPrecioOriginal() {
        MercanciaController mercanciaController = new MercanciaController(mercancias, vehiculos);
        Envio envio = controller.crear(ana, luis, sobreId, FECHA);
        Mercancia mercancia = mercanciaController.buscarPorId(sobreId);
        mercancia.setPrecioEnvio(new BigDecimal("99.00"));

        assertThrows(MercanciaConEnvioException.class, () -> mercanciaController.modificar(mercancia));
        Envio conIntento = controller.registrarIntentoFallido(envio.getId());

        assertEquals(new BigDecimal("21.00"), conIntento.getPrecioTotal());
        assertTotal("21.00", ana);
    }
}
