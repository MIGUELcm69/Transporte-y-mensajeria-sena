package com.transportes.controller;

import com.transportes.exception.DatoInvalidoException;
import com.transportes.exception.EntidadNoEncontradaException;
import com.transportes.exception.RegistroDuplicadoException;
import com.transportes.exception.SupervisorConVehiculosException;
import com.transportes.model.Supervisor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.transportes.controller.VehiculoDaoEnMemoria.avion;
import static com.transportes.controller.VehiculoDaoEnMemoria.moto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupervisorControllerTest {

    private final SupervisorDaoEnMemoria supervisores = new SupervisorDaoEnMemoria();
    private final VehiculoDaoEnMemoria vehiculos = new VehiculoDaoEnMemoria();
    private final TransaccionesEnMemoria transacciones = new TransaccionesEnMemoria(supervisores, vehiculos);
    private final SupervisorController controller = new SupervisorController(supervisores, vehiculos, transacciones);

    @BeforeEach
    void conectarDaos() {
        supervisores.usarVehiculos(vehiculos);
    }

    private static Supervisor nuevo(String nif) {
        return new Supervisor(null, nif, "Ana", "Calle 2", "600111222");
    }

    @Test
    void registrarGuardaSupervisorYPrimerVehiculoEnUnaTransaccion() {
        FichaSupervisor ficha = controller.registrar(nuevo("11111111A"), id -> moto("M-1", id));

        int id = ficha.supervisor().getId();
        assertTrue(supervisores.findById(id).isPresent());
        assertEquals(1, vehiculos.contarPorSupervisor(id));
        assertEquals(1, transacciones.confirmadas);
        assertEquals(0, supervisores.escriturasSinTransaccion + vehiculos.escriturasSinTransaccion);
    }

    @Test
    void registrarRevierteElSupervisorSiFallaElPrimerVehiculo() {
        Supervisor existente = supervisores.sembrar("00000000Z");
        vehiculos.sembrar(moto("M-1", existente.getId()));

        assertThrows(RegistroDuplicadoException.class,
                () -> controller.registrar(nuevo("11111111A"), id -> moto("M-1", id)));

        assertTrue(supervisores.findByNif("11111111A").isEmpty());
        assertEquals(1, transacciones.revertidas);
    }

    @Test
    void registrarRechazaPrimerVehiculoAsignadoAOtroSupervisor() {
        Supervisor existente = supervisores.sembrar("00000000Z");

        assertThrows(IllegalArgumentException.class,
                () -> controller.registrar(nuevo("11111111A"), id -> moto("M-1", existente.getId())));

        assertTrue(supervisores.findByNif("11111111A").isEmpty());
        assertTrue(vehiculos.findByCodigo("M-1").isEmpty());
    }

    @Test
    void eliminarRechazaSupervisorConVehiculos() {
        Supervisor supervisor = supervisores.sembrar("00000000Z");
        vehiculos.sembrar(moto("M-1", supervisor.getId()));

        assertThrows(SupervisorConVehiculosException.class, () -> controller.eliminar(supervisor.getId()));
        assertTrue(supervisores.findById(supervisor.getId()).isPresent());
    }

    @Test
    void eliminarBorraSupervisorSinVehiculos() {
        Supervisor supervisor = supervisores.sembrar("00000000Z");

        controller.eliminar(supervisor.getId());

        assertTrue(supervisores.findById(supervisor.getId()).isEmpty());
    }

    @Test
    void eliminarTraspasandoMueveLosVehiculosYBorraEnUnaTransaccion() {
        Supervisor origen = supervisores.sembrar("00000000Z");
        Supervisor destino = supervisores.sembrar("11111111A");
        vehiculos.sembrar(moto("M-1", origen.getId()));
        vehiculos.sembrar(avion("A-1", origen.getId()));
        vehiculos.sembrar(moto("M-2", destino.getId()));

        int traspasados = controller.eliminarTraspasandoVehiculos(origen.getId(), destino.getId());

        assertEquals(2, traspasados);
        assertEquals(3, vehiculos.contarPorSupervisor(destino.getId()));
        assertTrue(supervisores.findById(origen.getId()).isEmpty());
        assertEquals(1, transacciones.confirmadas);
        assertEquals(0, supervisores.escriturasSinTransaccion + vehiculos.escriturasSinTransaccion);
    }

    @Test
    void eliminarTraspasandoRevierteLosTraspasosSiFallaElBorrado() {
        Supervisor origen = supervisores.sembrar("00000000Z");
        Supervisor destino = supervisores.sembrar("11111111A");
        vehiculos.sembrar(moto("M-1", origen.getId()));
        supervisores.fallarAlEliminar = true;

        assertThrows(IllegalStateException.class,
                () -> controller.eliminarTraspasandoVehiculos(origen.getId(), destino.getId()));

        assertEquals(1, vehiculos.contarPorSupervisor(origen.getId()));
        assertEquals(0, vehiculos.contarPorSupervisor(destino.getId()));
        assertEquals(1, transacciones.revertidas);
    }

    @Test
    void eliminarTraspasandoExigeDestinoDistinto() {
        Supervisor origen = supervisores.sembrar("00000000Z");

        assertThrows(DatoInvalidoException.class,
                () -> controller.eliminarTraspasandoVehiculos(origen.getId(), origen.getId()));
    }

    @Test
    void eliminarTraspasandoExigeDestinoExistente() {
        Supervisor origen = supervisores.sembrar("00000000Z");
        vehiculos.sembrar(moto("M-1", origen.getId()));

        assertThrows(EntidadNoEncontradaException.class,
                () -> controller.eliminarTraspasandoVehiculos(origen.getId(), 99));

        assertTrue(supervisores.findById(origen.getId()).isPresent());
        assertEquals(1, vehiculos.contarPorSupervisor(origen.getId()));
        assertEquals(0, transacciones.confirmadas + transacciones.revertidas);
    }

    @Test
    void consultarDevuelveVehiculosYNumeroDeAviones() {
        Supervisor supervisor = supervisores.sembrar("00000000Z");
        vehiculos.sembrar(moto("M-1", supervisor.getId()));
        vehiculos.sembrar(avion("A-1", supervisor.getId()));
        vehiculos.sembrar(avion("A-2", supervisor.getId()));

        FichaSupervisor ficha = controller.consultar(supervisor.getId());

        assertEquals(3, ficha.vehiculos().size());
        assertEquals(2, ficha.numeroAviones());
        assertEquals(2, controller.contarAviones(supervisor.getId()));
    }
}
