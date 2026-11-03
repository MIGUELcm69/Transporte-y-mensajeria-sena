package com.transportes.view;

import com.transportes.exception.AccesoDatosException;
import com.transportes.exception.ConfiguracionException;
import com.transportes.exception.DomainException;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Único punto donde una excepción se convierte en mensaje para el usuario. Ninguna llega a la consola
 * con stack trace. Al log solo van tipo y mensaje; los errores de dominio no se registran, porque su
 * mensaje puede incluir datos personales (p. ej. el NIF buscado).
 */
public class ManejadorErrores {

    private static final Logger LOG = Logger.getLogger(ManejadorErrores.class.getName());

    static final String MENSAJE_CONFIGURACION = "No se pudo cargar la configuración de la base de datos. "
            + "Revise config.properties o las variables de entorno.";
    static final String MENSAJE_ACCESO_DATOS = "No se pudo completar la operación en la base de datos. "
            + "Compruebe que el servidor MySQL esté activo e inténtelo de nuevo.";
    static final String MENSAJE_INESPERADO = "Ha ocurrido un error inesperado. La operación no se ha realizado.";

    private final Consola consola;

    public ManejadorErrores(Consola consola) {
        this.consola = Objects.requireNonNull(consola, "consola");
    }

    /** {@link FinDeEntrada} se deja pasar para que el menú principal termine. */
    public void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (FinDeEntrada e) {
            throw e;
        } catch (OperacionCancelada e) {
            consola.mostrar(e.getMessage());
        } catch (DomainException e) {
            consola.error(e.getMessage());
        } catch (ConfiguracionException e) {
            registrar(e);
            consola.error(MENSAJE_CONFIGURACION);
        } catch (AccesoDatosException e) {
            registrar(e);
            consola.error(MENSAJE_ACCESO_DATOS);
        } catch (RuntimeException e) {
            registrar(e);
            consola.error(MENSAJE_INESPERADO);
        }
    }

    private static void registrar(RuntimeException e) {
        LOG.log(Level.SEVERE, e.getClass().getSimpleName() + ": " + e.getMessage());
    }
}
