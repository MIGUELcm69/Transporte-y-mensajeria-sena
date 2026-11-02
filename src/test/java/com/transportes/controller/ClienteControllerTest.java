package com.transportes.controller;

import com.transportes.exception.ClienteConEnviosException;
import com.transportes.exception.DatoInvalidoException;
import com.transportes.model.Cliente;
import com.transportes.model.ClienteEmpresa;
import com.transportes.model.ClienteParticular;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static com.transportes.controller.ClienteDaoEnMemoria.FECHA_ALTA;
import static com.transportes.controller.ClienteDaoEnMemoria.empresa;
import static com.transportes.controller.ClienteDaoEnMemoria.particular;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteControllerTest {

    private static final LocalDate HOY = LocalDate.of(2026, 3, 10);
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-10T12:00:00Z"), ZoneOffset.UTC);

    private final EnvioDaoEnMemoria envios = new EnvioDaoEnMemoria();
    private final ClienteDaoEnMemoria clientes = new ClienteDaoEnMemoria(envios);
    private final ClienteController controller = new ClienteController(clientes, envios, RELOJ);

    @Test
    void registrarParticularFijaFechaDeHoyYTotalCero() {
        ClienteParticular cliente = controller.registrarParticular("C-1", "12345678Z", "Ana", "Calle 1");

        Cliente guardado = controller.buscarPorId(cliente.getId());
        assertEquals(HOY, guardado.getFechaAlta());
        assertEquals(0, BigDecimal.ZERO.compareTo(guardado.getTotalFacturado()));
    }

    @Test
    void registrarEmpresaFijaFechaDeHoyYTotalCero() {
        ClienteEmpresa cliente = controller.registrarEmpresa("C-2", "B12345678", "Acme", "Calle 2", "910000000");

        Cliente guardado = controller.buscarPorCodigo("C-2");
        assertEquals(cliente.getId(), guardado.getId());
        assertEquals(HOY, guardado.getFechaAlta());
        assertEquals(0, BigDecimal.ZERO.compareTo(guardado.getTotalFacturado()));
    }

    @Test
    void modificarActualizaDatosEditables() {
        ClienteParticular cliente = clientes.sembrar(particular("C-1", "12345678Z"));
        cliente.setDireccion("Calle Nueva 9");

        controller.modificar(cliente);

        assertEquals("Calle Nueva 9", controller.buscarPorId(cliente.getId()).getDireccion());
    }

    @Test
    void modificarNoPermiteCambiarElTipo() {
        ClienteParticular cliente = clientes.sembrar(particular("C-1", "12345678Z"));
        ClienteEmpresa comoEmpresa = new ClienteEmpresa(cliente.getId(), "C-1", FECHA_ALTA, BigDecimal.ZERO,
                "B12345678", "Ana", "Calle 3", "910000000");

        DatoInvalidoException error = assertThrows(DatoInvalidoException.class,
                () -> controller.modificar(comoEmpresa));

        assertTrue(error.getMessage().contains("registrar un cliente nuevo"), error.getMessage());
        assertTrue(controller.buscarPorId(cliente.getId()) instanceof ClienteParticular);
    }

    @Test
    void modificarNoPermiteCambiarElCodigo() {
        ClienteParticular cliente = clientes.sembrar(particular("C-1", "12345678Z"));
        ClienteParticular conOtroCodigo = new ClienteParticular(cliente.getId(), "C-9", FECHA_ALTA,
                BigDecimal.ZERO, "12345678Z", "Ana", "Calle 3");

        assertThrows(DatoInvalidoException.class, () -> controller.modificar(conOtroCodigo));
    }

    @Test
    void eliminarRechazaClienteConEnviosComoOrigen() {
        Cliente origen = clientes.sembrar(particular("C-1", "12345678Z"));
        Cliente destinatario = clientes.sembrar(empresa("C-2", "B12345678"));
        envios.sembrar(origen.getId(), destinatario.getId(), 1, HOY, "20.00");

        assertThrows(ClienteConEnviosException.class, () -> controller.eliminar(origen.getId()));
        assertTrue(clientes.findById(origen.getId()).isPresent());
    }

    @Test
    void eliminarRechazaClienteConEnviosComoDestinatario() {
        Cliente origen = clientes.sembrar(particular("C-1", "12345678Z"));
        Cliente destinatario = clientes.sembrar(empresa("C-2", "B12345678"));
        envios.sembrar(origen.getId(), destinatario.getId(), 1, HOY, "20.00");

        assertThrows(ClienteConEnviosException.class, () -> controller.eliminar(destinatario.getId()));
        assertTrue(clientes.findById(destinatario.getId()).isPresent());
    }

    @Test
    void eliminarBorraClienteSinEnvios() {
        Cliente cliente = clientes.sembrar(particular("C-1", "12345678Z"));

        controller.eliminar(cliente.getId());

        assertTrue(clientes.findById(cliente.getId()).isEmpty());
    }

    @Test
    void historialDevuelveLosEnviosEncargadosPorFechaYElTotalFacturado() {
        Cliente ana = clientes.sembrar(particular("C-1", "12345678Z"));
        Cliente acme = clientes.sembrar(empresa("C-2", "B12345678"));
        clientes.sumarTotalFacturado(ana.getId(), new BigDecimal("50.00"));
        envios.sembrar(ana.getId(), acme.getId(), 1, HOY, "30.00");
        envios.sembrar(ana.getId(), acme.getId(), 2, HOY.minusDays(5), "20.00");
        envios.sembrar(acme.getId(), ana.getId(), 3, HOY, "10.40");

        HistorialCliente historial = controller.historial(ana.getId());

        assertEquals(2, historial.envios().size());
        assertEquals(HOY.minusDays(5), historial.envios().get(0).getFechaEnvio());
        assertEquals(new BigDecimal("50.00"), historial.totalFacturado());
    }
}
