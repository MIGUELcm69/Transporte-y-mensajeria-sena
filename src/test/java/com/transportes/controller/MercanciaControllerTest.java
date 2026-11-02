package com.transportes.controller;

import com.transportes.exception.EntidadNoEncontradaException;
import com.transportes.exception.MercanciaConEnvioException;
import com.transportes.exception.ReglaAsignacionException;
import com.transportes.model.Mercancia;
import com.transportes.model.Sobre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.transportes.controller.MercanciaDaoEnMemoria.PRECIO_ENVIO;
import static com.transportes.controller.MercanciaDaoEnMemoria.paquete;
import static com.transportes.controller.MercanciaDaoEnMemoria.sobre;
import static com.transportes.controller.MercanciaDaoEnMemoria.sobreLargoRecorrido;
import static com.transportes.controller.VehiculoDaoEnMemoria.avion;
import static com.transportes.controller.VehiculoDaoEnMemoria.furgoneta;
import static com.transportes.controller.VehiculoDaoEnMemoria.moto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MercanciaControllerTest {

    private static final int SUPERVISOR = 1;

    private final EnvioDaoEnMemoria envios = new EnvioDaoEnMemoria();
    private final MercanciaDaoEnMemoria mercancias = new MercanciaDaoEnMemoria(envios);
    private final VehiculoDaoEnMemoria vehiculos = new VehiculoDaoEnMemoria();
    private final MercanciaController controller = new MercanciaController(mercancias, vehiculos);

    private int motoId;
    private int furgonetaId;
    private int avionId;

    @BeforeEach
    void sembrarVehiculos() {
        motoId = vehiculos.sembrar(moto("M-1", SUPERVISOR)).getId();
        furgonetaId = vehiculos.sembrar(furgoneta("F-1", SUPERVISOR)).getId();
        avionId = vehiculos.sembrar(avion("A-1", SUPERVISOR)).getId();
    }

    private void asociarEnvio(Mercancia mercancia) {
        envios.sembrar(1, 2, mercancia.getId(), LocalDate.of(2026, 3, 1), "20.80");
    }

    @Test
    void registrarGuardaSobreEnMotoYPaqueteEnFurgoneta() {
        controller.registrar(sobre("S-1", motoId));
        controller.registrar(paquete("P-1", furgonetaId));

        assertEquals(2, controller.listarTodos().size());
    }

    @Test
    void registrarRechazaSobreEnFurgoneta() {
        assertThrows(ReglaAsignacionException.class, () -> controller.registrar(sobre("S-1", furgonetaId)));
        assertTrue(mercancias.findByCodigo("S-1").isEmpty());
    }

    @Test
    void registrarRechazaPaqueteEnMoto() {
        assertThrows(ReglaAsignacionException.class, () -> controller.registrar(paquete("P-1", motoId)));
        assertTrue(mercancias.findByCodigo("P-1").isEmpty());
    }

    @Test
    void registrarExigeVehiculoExistente() {
        assertThrows(EntidadNoEncontradaException.class, () -> controller.registrar(sobre("S-1", 99)));
    }

    @Test
    void registrarGuardaLargoRecorridoConAvionYVehiculoDeDestino() {
        int otraMotoId = vehiculos.sembrar(moto("M-2", SUPERVISOR)).getId();

        Mercancia mercancia = controller.registrar(sobreLargoRecorrido("S-1", motoId, avionId, otraMotoId));

        assertTrue(controller.buscarPorId(mercancia.getId()).isLargoRecorrido());
    }

    @Test
    void registrarRechazaLargoRecorridoSinAvionDeTipoAvion() {
        assertThrows(ReglaAsignacionException.class,
                () -> controller.registrar(sobreLargoRecorrido("S-1", motoId, furgonetaId, motoId)));
    }

    @Test
    void registrarRechazaVehiculoDeDestinoDeOtroTipo() {
        assertThrows(ReglaAsignacionException.class,
                () -> controller.registrar(sobreLargoRecorrido("S-1", motoId, avionId, furgonetaId)));
    }

    @Test
    void modificarRechazaCambiarPrecioSiHayEnvio() {
        Sobre mercancia = mercancias.sembrar(sobre("S-1", motoId));
        asociarEnvio(mercancia);
        mercancia.setPrecioEnvio(new BigDecimal("99.00"));

        assertThrows(MercanciaConEnvioException.class, () -> controller.modificar(mercancia));
        assertEquals(PRECIO_ENVIO, controller.buscarPorId(mercancia.getId()).getPrecioEnvio());
    }

    @Test
    void modificarPermiteOtrosCambiosSiHayEnvio() {
        Sobre mercancia = mercancias.sembrar(sobre("S-1", motoId));
        asociarEnvio(mercancia);
        mercancia.setDescripcion("Contrato firmado");
        mercancia.setAsegurada(true);

        controller.modificar(mercancia);

        Mercancia guardada = controller.buscarPorId(mercancia.getId());
        assertEquals("Contrato firmado", guardada.getDescripcion());
        assertTrue(guardada.isAsegurada());
    }

    @Test
    void modificarPermiteCambiarPrecioSinEnvio() {
        Sobre mercancia = mercancias.sembrar(sobre("S-1", motoId));
        mercancia.setPrecioEnvio(new BigDecimal("25.00"));

        controller.modificar(mercancia);

        assertEquals(new BigDecimal("25.00"), controller.buscarPorId(mercancia.getId()).getPrecioEnvio());
    }

    @Test
    void modificarValidaLosVehiculosNuevos() {
        Sobre mercancia = mercancias.sembrar(sobre("S-1", motoId));
        mercancia.asignarVehiculos(false, furgonetaId, null, null);

        assertThrows(ReglaAsignacionException.class, () -> controller.modificar(mercancia));
        assertEquals(motoId, controller.buscarPorId(mercancia.getId()).getVehiculoId());
    }

    @Test
    void eliminarRechazaMercanciaConEnvio() {
        Sobre mercancia = mercancias.sembrar(sobre("S-1", motoId));
        asociarEnvio(mercancia);

        assertThrows(MercanciaConEnvioException.class, () -> controller.eliminar(mercancia.getId()));
        assertTrue(mercancias.findById(mercancia.getId()).isPresent());
    }

    @Test
    void eliminarBorraMercanciaSinEnvio() {
        Sobre mercancia = mercancias.sembrar(sobre("S-1", motoId));

        controller.eliminar(mercancia.getId());

        assertTrue(mercancias.findById(mercancia.getId()).isEmpty());
    }
}
