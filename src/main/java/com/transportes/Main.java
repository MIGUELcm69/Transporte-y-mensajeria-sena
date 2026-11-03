package com.transportes;

import com.transportes.config.RegistroLog;
import com.transportes.controller.ClienteController;
import com.transportes.controller.EnvioController;
import com.transportes.controller.MercanciaController;
import com.transportes.controller.SupervisorController;
import com.transportes.controller.Transacciones;
import com.transportes.controller.TransaccionesJdbc;
import com.transportes.controller.VehiculoController;
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
import com.transportes.view.ClienteView;
import com.transportes.view.Consola;
import com.transportes.view.EnvioView;
import com.transportes.view.ManejadorErrores;
import com.transportes.view.MenuPrincipal;
import com.transportes.view.MercanciaView;
import com.transportes.view.ReportesView;
import com.transportes.view.SupervisorView;
import com.transportes.view.VehiculoView;

import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

/** Raíz de composición: crea DAO, transacciones, controladores y vistas, y arranca el menú. */
public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        boolean logActivo = RegistroLog.configurar();
        LOG.info("Aplicación iniciada");
        try {
            arrancar(logActivo);
        } finally {
            LOG.info("Aplicación finalizada");
        }
    }

    private static void arrancar(boolean logActivo) {
        Consola consola = new Consola(new Scanner(System.in), System.out);
        if (!logActivo) {
            consola.error("No se pudo abrir el archivo de log logs/transportes.log; "
                    + "la aplicación funcionará sin registrar errores.");
        }
        ManejadorErrores manejador = new ManejadorErrores(consola);

        SupervisorDao supervisorDao = new SupervisorDaoImpl();
        VehiculoDao vehiculoDao = new VehiculoDaoImpl();
        ClienteDao clienteDao = new ClienteDaoImpl();
        MercanciaDao mercanciaDao = new MercanciaDaoImpl();
        EnvioDao envioDao = new EnvioDaoImpl();
        Transacciones transacciones = new TransaccionesJdbc();

        VehiculoController vehiculoController = new VehiculoController(vehiculoDao, supervisorDao);
        SupervisorController supervisorController =
                new SupervisorController(supervisorDao, vehiculoDao, transacciones);
        ClienteController clienteController = new ClienteController(clienteDao, envioDao);
        MercanciaController mercanciaController = new MercanciaController(mercanciaDao, vehiculoDao);
        EnvioController envioController = new EnvioController(envioDao, clienteDao, mercanciaDao, transacciones);

        new MenuPrincipal(consola, manejador, List.of(
                new VehiculoView(consola, manejador, vehiculoController, supervisorController),
                new SupervisorView(consola, manejador, supervisorController),
                new ClienteView(consola, manejador, clienteController, mercanciaController),
                new MercanciaView(consola, manejador, mercanciaController, vehiculoController),
                new EnvioView(consola, manejador, envioController, clienteController, mercanciaController),
                new ReportesView(consola, manejador, mercanciaController, vehiculoController, clienteController)
        )).iniciar();
    }
}
