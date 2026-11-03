package com.transportes.config;

import com.transportes.exception.ConfiguracionException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Abre conexiones con la configuración de las variables de entorno o de config.properties.
 * La configuración solo se guarda si están los tres valores; si falta alguno, cada petición
 * vuelve a cargarla y lanza ConfiguracionException, sin intentar conectar.
 */
public class ConexionBD {

    @FunctionalInterface
    interface Conector {
        Connection conectar(String url, String usuario, String password) throws SQLException;
    }

    private record Configuracion(String url, String usuario, String password) {
    }

    private static final ConexionBD PREDETERMINADA =
            new ConexionBD(System::getenv, ConexionBD::leerArchivo, DriverManager::getConnection);

    private final UnaryOperator<String> entorno;
    private final Supplier<Properties> archivo;
    private final Conector conector;
    private Configuracion configuracion;

    ConexionBD(UnaryOperator<String> entorno, Supplier<Properties> archivo, Conector conector) {
        this.entorno = entorno;
        this.archivo = archivo;
        this.conector = conector;
    }

    public static Connection getConnection() throws SQLException {
        return PREDETERMINADA.abrir();
    }

    Connection abrir() throws SQLException {
        Configuracion actual = configuracion();
        return conector.conectar(actual.url(), actual.usuario(), actual.password());
    }

    private synchronized Configuracion configuracion() {
        if (configuracion == null) {
            configuracion = cargarConfiguracion();
        }
        return configuracion;
    }

    private Configuracion cargarConfiguracion() {
        Properties props = archivo.get();
        String url      = obtener("DB_URL",      props, "db.url",      "URL de la base de datos");
        String usuario  = obtener("DB_USUARIO",  props, "db.usuario",  "usuario de la base de datos");
        String password = obtener("DB_PASSWORD", props, "db.password", "contraseña de la base de datos");
        return new Configuracion(url, usuario, password);
    }

    private static Properties leerArchivo() {
        Properties props = new Properties();
        try (InputStream is = ConexionBD.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            throw new ConfiguracionException(
                    "No se pudo leer el archivo de configuración.");
        }
        return props;
    }

    private String obtener(String varEntorno, Properties props,
                           String clave, String descripcion) {
        String valor = entorno.apply(varEntorno);
        if (valor == null || valor.trim().isEmpty()) {
            valor = props.getProperty(clave);
        }
        if (valor == null || valor.trim().isEmpty()) {
            throw new ConfiguracionException(
                    "Falta la " + descripcion + ". " +
                    "Configura la variable de entorno " + varEntorno +
                    " o la clave '" + clave + "' en config.properties.");
        }
        return valor.trim();
    }
}
