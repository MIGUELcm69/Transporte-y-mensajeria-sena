package com.transportes;

import com.transportes.exception.AccesoDatosException;
import com.transportes.exception.ConfiguracionException;
import com.transportes.exception.DomainException;
import com.transportes.model.Cliente;
import com.transportes.model.ClienteEmpresa;
import com.transportes.model.ClienteParticular;
import com.transportes.model.Envio;
import com.transportes.model.Furgoneta;
import com.transportes.model.Mercancia;
import com.transportes.model.Moto;
import com.transportes.model.Supervisor;
import com.transportes.model.Vehiculo;
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

import java.util.Optional;

/**
 * Prueba manual de solo lectura de la capa DAO contra la BD de pruebas.
 * No invoca save, update ni delete.
 */
public class PruebaLectura {

    private static final String CODIGO_INEXISTENTE = "NO-EXISTE-999";

    private final VehiculoDao vehiculoDao = new VehiculoDaoImpl();
    private final ClienteDao clienteDao = new ClienteDaoImpl();
    private final MercanciaDao mercanciaDao = new MercanciaDaoImpl();
    private final EnvioDao envioDao = new EnvioDaoImpl();
    private final SupervisorDao supervisorDao = new SupervisorDaoImpl();

    public static void main(String[] args) {
        try {
            new PruebaLectura().ejecutar();
        } catch (DomainException e) {
            System.err.println("No se pudo completar la consulta: " + e.getMessage());
        } catch (AccesoDatosException e) {
            System.err.println("No se pudo acceder a los datos. Verifique que el servidor MySQL esté activo "
                    + "e inténtelo de nuevo.");
        } catch (ConfiguracionException e) {
            System.err.println("Error de configuración: " + e.getMessage());
        }
    }

    private void ejecutar() {
        mostrarVehiculos();
        mostrarClientes();
        mostrarMercancias();
        mostrarEnvios();
        mostrarAvionesPorSupervisor();
        probarCodigoInexistente();
    }

    private void mostrarVehiculos() {
        titulo("Vehículos");
        for (Vehiculo v : vehiculoDao.findAll()) {
            System.out.printf("  [%d] %s %-9s %-30s %s%n",
                    v.getId(), v.getCodigo(), v.getTipo(), v.getDescripcion(), detalleVehiculo(v));
        }
    }

    private String detalleVehiculo(Vehiculo v) {
        if (v instanceof Moto moto) {
            return "cilindrada=" + moto.getCilindrada();
        }
        if (v instanceof Furgoneta furgoneta) {
            return "capacidad=" + furgoneta.getCapacidad();
        }
        return "";
    }

    private void mostrarClientes() {
        titulo("Clientes");
        for (Cliente c : clienteDao.findAll()) {
            System.out.printf("  [%d] %s %-10s %-25s %s total_facturado=%s%n",
                    c.getId(), c.getCodigoCliente(), c.getTipo(), c.getNombre(), documentoCliente(c),
                    c.getTotalFacturado());
        }
    }

    private String documentoCliente(Cliente c) {
        if (c instanceof ClienteParticular particular) {
            return "NIF=" + particular.getNif();
        }
        if (c instanceof ClienteEmpresa empresa) {
            return "CIF=" + empresa.getCif();
        }
        return "";
    }

    private void mostrarMercancias() {
        titulo("Mercancías");
        for (Mercancia m : mercanciaDao.findAll()) {
            String asignacion = m.isLargoRecorrido()
                    ? "origen=" + m.getVehiculoId() + " avión=" + m.getAvionId()
                            + " destino=" + m.getVehiculoDestinoId()
                    : "vehículo=" + m.getVehiculoId();
            System.out.printf("  [%d] %s %-7s largo_recorrido=%-5s %s%n",
                    m.getId(), m.getCodigo(), m.getTipo(), m.isLargoRecorrido(), asignacion);
        }
    }

    private void mostrarEnvios() {
        titulo("Envíos");
        for (Envio e : envioDao.findAll()) {
            System.out.printf("  [%d] origen=%d destinatario=%d mercancía=%d fecha=%s precio_total=%s intentos_fallidos=%d%n",
                    e.getId(), e.getClienteOrigenId(), e.getDestinatarioId(), e.getMercanciaId(),
                    e.getFechaEnvio(), e.getPrecioTotal(), e.getIntentosFallidos());
        }
    }

    private void mostrarAvionesPorSupervisor() {
        titulo("Aviones por supervisor");
        for (Supervisor s : supervisorDao.findAll()) {
            System.out.printf("  [%d] %s %-25s aviones=%d%n",
                    s.getId(), s.getNif(), s.getNombre(), supervisorDao.contarAviones(s.getId()));
        }
    }

    private void probarCodigoInexistente() {
        titulo("findByCodigo inexistente (" + CODIGO_INEXISTENTE + ")");
        Optional<Vehiculo> vehiculo = vehiculoDao.findByCodigo(CODIGO_INEXISTENTE);
        Optional<Cliente> cliente = clienteDao.findByCodigo(CODIGO_INEXISTENTE);
        Optional<Mercancia> mercancia = mercanciaDao.findByCodigo(CODIGO_INEXISTENTE);
        System.out.println("  Vehículo  -> " + (vehiculo.isEmpty() ? "Optional vacío (correcto)" : vehiculo.get()));
        System.out.println("  Cliente   -> " + (cliente.isEmpty() ? "Optional vacío (correcto)" : cliente.get()));
        System.out.println("  Mercancía -> " + (mercancia.isEmpty() ? "Optional vacío (correcto)" : mercancia.get()));
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("=== " + texto + " ===");
    }
}
