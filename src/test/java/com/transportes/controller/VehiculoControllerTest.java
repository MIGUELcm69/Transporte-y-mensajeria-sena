package com.transportes.controller;

import com.transportes.exception.DatoInvalidoException;
import com.transportes.exception.EntidadNoEncontradaException;
import com.transportes.exception.UltimoVehiculoException;
import com.transportes.exception.VehiculoEnUsoException;
import com.transportes.model.Moto;
import com.transportes.model.Supervisor;
import com.transportes.model.Vehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.transportes.controller.VehiculoDaoEnMemoria.FECHA;
import static com.transportes.controller.VehiculoDaoEnMemoria.PRECIO;
import static com.transportes.controller.VehiculoDaoEnMemoria.moto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehiculoControllerTest {

    private final SupervisorDaoEnMemoria supervisores = new SupervisorDaoEnMemoria();
    private final VehiculoDaoEnMemoria vehiculos = new VehiculoDaoEnMemoria();
    private final VehiculoController controller = new VehiculoController(vehiculos, supervisores);

    private Supervisor ana;
    private Supervisor luis;

    @BeforeEach
    void sembrar() {
        ana = supervisores.sembrar("00000000Z");
        luis = supervisores.sembrar("11111111A");
    }

    @Test
    void registrarExigeSupervisorExistente() {
        assertThrows(EntidadNoEncontradaException.class, () -> controller.registrar(moto("M-1", 99)));
        assertTrue(vehiculos.findByCodigo("M-1").isEmpty());
    }

    @Test
    void registrarGuardaVehiculoDeSupervisorExistente() {
        Vehiculo vehiculo = controller.registrar(moto("M-1", ana.getId()));

        assertEquals(vehiculo.getId(), controller.buscarPorCodigo("M-1").getId());
    }

    @Test
    void eliminarRechazaVehiculoConMercancias() {
        Vehiculo vehiculo = vehiculos.sembrar(moto("M-1", ana.getId()));
        vehiculos.sembrar(moto("M-2", ana.getId()));
        vehiculos.marcarConMercancias(vehiculo.getId());

        assertThrows(VehiculoEnUsoException.class, () -> controller.eliminar(vehiculo.getId()));
        assertTrue(vehiculos.findById(vehiculo.getId()).isPresent());
    }

    @Test
    void eliminarRechazaUltimoVehiculoDelSupervisor() {
        Vehiculo vehiculo = vehiculos.sembrar(moto("M-1", ana.getId()));

        assertThrows(UltimoVehiculoException.class, () -> controller.eliminar(vehiculo.getId()));
        assertTrue(vehiculos.findById(vehiculo.getId()).isPresent());
    }

    @Test
    void eliminarBorraSiAlSupervisorLeQuedanOtrosVehiculos() {
        Vehiculo vehiculo = vehiculos.sembrar(moto("M-1", ana.getId()));
        vehiculos.sembrar(moto("M-2", ana.getId()));

        controller.eliminar(vehiculo.getId());

        assertTrue(vehiculos.findById(vehiculo.getId()).isEmpty());
    }

    @Test
    void modificarRechazaReasignarUltimoVehiculoDelSupervisor() {
        Vehiculo vehiculo = vehiculos.sembrar(moto("M-1", ana.getId()));
        vehiculo.setSupervisorId(luis.getId());

        assertThrows(UltimoVehiculoException.class, () -> controller.modificar(vehiculo));
        assertEquals(ana.getId(), controller.buscarPorId(vehiculo.getId()).getSupervisorId());
    }

    @Test
    void modificarReasignaSiAlSupervisorLeQuedanOtrosVehiculos() {
        Vehiculo vehiculo = vehiculos.sembrar(moto("M-1", ana.getId()));
        vehiculos.sembrar(moto("M-2", ana.getId()));
        vehiculo.setSupervisorId(luis.getId());

        controller.modificar(vehiculo);

        assertEquals(luis.getId(), controller.buscarPorId(vehiculo.getId()).getSupervisorId());
    }

    @Test
    void modificarExigeSupervisorDestinoExistente() {
        Vehiculo vehiculo = vehiculos.sembrar(moto("M-1", ana.getId()));
        vehiculos.sembrar(moto("M-2", ana.getId()));
        vehiculo.setSupervisorId(99);

        assertThrows(EntidadNoEncontradaException.class, () -> controller.modificar(vehiculo));
    }

    @Test
    void modificarRechazaCambiarElCodigo() {
        Vehiculo vehiculo = vehiculos.sembrar(moto("M-1", ana.getId()));
        Vehiculo conOtroCodigo = new Moto(vehiculo.getId(), "M-9", "Moto", FECHA, PRECIO, ana.getId(),
                new BigDecimal("125.00"));

        assertThrows(DatoInvalidoException.class, () -> controller.modificar(conOtroCodigo));
    }
}
