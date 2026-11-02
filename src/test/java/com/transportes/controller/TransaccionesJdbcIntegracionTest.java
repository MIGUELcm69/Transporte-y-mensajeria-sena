package com.transportes.controller;

import com.transportes.config.ConexionBD;
import com.transportes.exception.RegistroDuplicadoException;
import com.transportes.model.Cliente;
import com.transportes.model.ClienteParticular;
import com.transportes.model.Envio;
import com.transportes.model.Moto;
import com.transportes.model.Sobre;
import com.transportes.model.Supervisor;
import com.transportes.model.dao.ClienteDao;
import com.transportes.model.dao.ClienteDaoImpl;
import com.transportes.model.dao.EnvioDao;
import com.transportes.model.dao.EnvioDaoImpl;
import com.transportes.model.dao.MercanciaDao;
import com.transportes.model.dao.MercanciaDaoImpl;
import com.transportes.model.dao.SupervisorDao;
import com.transportes.model.dao.SupervisorDaoImpl;
import com.transportes.model.dao.VehiculoDao;
import com.transportes.model.dao.VehiculoDaoImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test de integración contra MySQL real. Solo se ejecuta si DB_URL apunta al esquema transportes_test;
 * en cualquier otro caso se omite. Cada test borra todas las filas de las cinco tablas antes de empezar.
 *
 * Preparación (una vez): cargar sql/schema.sql en un esquema transportes_test (cambiando transportes_db
 * por transportes_test en CREATE DATABASE y USE) y conceder al usuario
 * GRANT SELECT, INSERT, UPDATE, DELETE ON transportes_test.*.
 *
 * Variables en IntelliJ:
 * - Al ejecutar el test desde el editor: Run > Edit Configurations... > la configuración JUnit del test
 *   (o Edit configuration templates... > JUnit, para todas) > Environment variables.
 * - Al ejecutar Maven > verify: Run > Edit Configurations... > la configuración Maven > pestaña Runner >
 *   desmarcar "Use project settings" > Environment variables.
 * Valor, separado por punto y coma:
 *   DB_URL=jdbc:mysql://localhost:3306/transportes_test;DB_USUARIO=transportes_app;DB_PASSWORD=tu_clave
 */
class TransaccionesJdbcIntegracionTest {

    private static final String ESQUEMA_PRUEBAS = "/transportes_test";
    /** Orden inverso de dependencias, por las FK con RESTRICT. */
    private static final List<String> BORRADOS = List.of(
            "DELETE FROM envio", "DELETE FROM mercancia", "DELETE FROM cliente",
            "DELETE FROM vehiculo", "DELETE FROM supervisor");
    private static final LocalDate FECHA = LocalDate.of(2026, 3, 1);
    private static final BigDecimal PRECIO_ENVIO = new BigDecimal("20.00");

    private final SupervisorDao supervisorDao = new SupervisorDaoImpl();
    private final VehiculoDao vehiculoDao = new VehiculoDaoImpl();
    private final ClienteDao clienteDao = new ClienteDaoImpl();
    private final MercanciaDao mercanciaDao = new MercanciaDaoImpl();
    private final EnvioDao envioDao = new EnvioDaoImpl();
    private final Transacciones transacciones = new TransaccionesJdbc();
    private final SupervisorController supervisorController =
            new SupervisorController(supervisorDao, vehiculoDao, transacciones);
    private final EnvioController envioController =
            new EnvioController(envioDao, clienteDao, mercanciaDao, transacciones);

    @BeforeAll
    static void exigirEsquemaDePruebas() {
        String url = System.getenv("DB_URL");
        assumeTrue(url != null && rutaSinParametros(url).endsWith(ESQUEMA_PRUEBAS),
                "Test de integración omitido: DB_URL debe apuntar a " + ESQUEMA_PRUEBAS
                        + " (nunca a transportes_db). Valor actual: " + (url == null ? "sin definir" : url));
    }

    private static String rutaSinParametros(String url) {
        int inicioParametros = url.indexOf('?');
        return inicioParametros < 0 ? url : url.substring(0, inicioParametros);
    }

    @BeforeEach
    void vaciarTablas() throws SQLException {
        exigirEsquemaDePruebas();
        try (Connection conexion = ConexionBD.getConnection()) {
            for (String borrado : BORRADOS) {
                try (PreparedStatement sentencia = conexion.prepareStatement(borrado)) {
                    sentencia.executeUpdate();
                }
            }
        }
    }

    // ----- Supervisor + primer vehículo -----

    @Test
    void altaDeSupervisorConPrimerVehiculoGuardaAmbos() {
        FichaSupervisor ficha = supervisorController.registrar(supervisor("11111111A"), id -> moto("MOTO-1", id));

        int supervisorId = ficha.supervisor().getId();
        assertTrue(supervisorDao.findById(supervisorId).isPresent());
        assertEquals(1, vehiculoDao.findBySupervisor(supervisorId).size());
        assertEquals("MOTO-1", vehiculoDao.findBySupervisor(supervisorId).get(0).getCodigo());
    }

    @Test
    void altaConVehiculoInvalidoRevierteYNoQuedaElSupervisor() {
        supervisorController.registrar(supervisor("11111111A"), id -> moto("MOTO-1", id));

        // El código repetido lo rechaza la BD (UNIQUE) después de insertar el supervisor nuevo.
        assertThrows(RegistroDuplicadoException.class,
                () -> supervisorController.registrar(supervisor("22222222B"), id -> moto("MOTO-1", id)));

        assertTrue(supervisorDao.findByNif("22222222B").isEmpty());
        assertEquals(1, supervisorDao.findAll().size());
        assertEquals(1, vehiculoDao.findAll().size());
    }

    // ----- Envío y total facturado -----

    @Test
    void crearEnvioSumaAlTotalFacturadoDelClienteOrigen() {
        Escenario e = sembrarEscenario();

        Envio envio = envioController.crear(e.origen(), e.destinatario(), e.sobre(), FECHA);

        // 20.00 + 20.00 × 0.04 (destinatario particular)
        assertEquals(0, new BigDecimal("20.80").compareTo(envio.getPrecioTotal()));
        assertTotal("20.80", e.origen());
        assertTotal("0", e.destinatario());
        assertTrue(envioDao.findById(envio.getId()).isPresent());
    }

    @Test
    void eliminarEnvioDevuelveElTotalFacturadoASuValorAnterior() {
        Escenario e = sembrarEscenario();
        envioController.crear(e.origen(), e.destinatario(), e.sobre(), FECHA);
        int otroSobre = sembrarSobre("SOBRE-2", e.moto());
        Envio segundo = envioController.crear(e.origen(), e.destinatario(), otroSobre, FECHA);
        assertTotal("41.60", e.origen());

        envioController.eliminar(segundo.getId());

        assertTotal("20.80", e.origen());
        assertTrue(envioDao.findById(segundo.getId()).isEmpty());
        assertEquals(1, envioDao.findAll().size());
    }

    @Test
    void falloAMitadDeLaTransaccionNoDejaNiEnvioNiCambioDeTotal() {
        Escenario e = sembrarEscenario();
        BigDecimal totalAntes = clienteDao.findById(e.origen()).orElseThrow().getTotalFacturado();
        EnvioController controllerQueFalla = new EnvioController(envioDao,
                new ClienteDaoQueFallaAlSumar(clienteDao), mercanciaDao, transacciones);

        assertThrows(IllegalStateException.class,
                () -> controllerQueFalla.crear(e.origen(), e.destinatario(), e.sobre(), FECHA));

        assertTrue(envioDao.findAll().isEmpty());
        assertFalse(mercanciaDao.tieneEnvio(e.sobre()));
        assertTotal(totalAntes.toPlainString(), e.origen());
    }

    // ----- Datos de apoyo -----

    /**
     * Delega todo en el DAO real. En sumarTotalFacturado ejecuta el UPDATE real y después falla,
     * de modo que el rollback debe deshacer tanto el INSERT del envío como el cambio de total.
     */
    private static final class ClienteDaoQueFallaAlSumar implements ClienteDao {

        private final ClienteDao real;

        ClienteDaoQueFallaAlSumar(ClienteDao real) {
            this.real = real;
        }

        @Override
        public boolean sumarTotalFacturado(int clienteId, BigDecimal delta, Connection conexion) {
            real.sumarTotalFacturado(clienteId, delta, conexion);
            throw new IllegalStateException("Fallo simulado tras actualizar el total facturado");
        }

        @Override
        public boolean sumarTotalFacturado(int clienteId, BigDecimal delta) {
            throw new IllegalStateException("Fallo simulado: sumar sin transacción");
        }

        @Override
        public void save(Cliente cliente) {
            real.save(cliente);
        }

        @Override
        public void save(Cliente cliente, Connection conexion) {
            real.save(cliente, conexion);
        }

        @Override
        public boolean update(Cliente cliente) {
            return real.update(cliente);
        }

        @Override
        public boolean update(Cliente cliente, Connection conexion) {
            return real.update(cliente, conexion);
        }

        @Override
        public boolean delete(int id) {
            return real.delete(id);
        }

        @Override
        public boolean delete(int id, Connection conexion) {
            return real.delete(id, conexion);
        }

        @Override
        public Optional<Cliente> findById(int id) {
            return real.findById(id);
        }

        @Override
        public Optional<Cliente> findByCodigo(String codigoCliente) {
            return real.findByCodigo(codigoCliente);
        }

        @Override
        public List<Cliente> findAll() {
            return real.findAll();
        }

        @Override
        public boolean tieneEnvios(int clienteId) {
            return real.tieneEnvios(clienteId);
        }
    }

    /** Ids de un supervisor con una moto, un sobre asignado a ella y dos clientes particulares. */
    private record Escenario(int moto, int sobre, int origen, int destinatario) {
    }

    /** Siembra con los DAO reales, cada uno con su propia conexión; sin pasar por los controladores. */
    private Escenario sembrarEscenario() {
        Supervisor supervisor = supervisor("11111111A");
        supervisorDao.save(supervisor);
        Moto moto = moto("MOTO-1", supervisor.getId());
        vehiculoDao.save(moto);
        return new Escenario(moto.getId(), sembrarSobre("SOBRE-1", moto.getId()),
                sembrarParticular("CLI-1", "33333333C"), sembrarParticular("CLI-2", "44444444D"));
    }

    private int sembrarSobre(String codigo, int motoId) {
        Sobre sobre = new Sobre(null, codigo, "Documentos", PRECIO_ENVIO, false, false,
                motoId, null, null, new BigDecimal("0.500"));
        mercanciaDao.save(sobre);
        return sobre.getId();
    }

    private int sembrarParticular(String codigo, String nif) {
        Cliente cliente = ClienteParticular.nuevo(codigo, FECHA, nif, "Cliente " + codigo, "Calle 1");
        clienteDao.save(cliente);
        return cliente.getId();
    }

    private static Supervisor supervisor(String nif) {
        return new Supervisor(null, nif, "Supervisor " + nif, "Calle 2", "3001234567");
    }

    private static Moto moto(String codigo, int supervisorId) {
        return new Moto(null, codigo, "Moto de reparto", FECHA, new BigDecimal("1500.00"), supervisorId,
                new BigDecimal("125.00"));
    }

    private void assertTotal(String esperado, int clienteId) {
        BigDecimal total = clienteDao.findById(clienteId).orElseThrow().getTotalFacturado();
        assertEquals(0, new BigDecimal(esperado).compareTo(total), "total facturado: " + total);
    }
}
