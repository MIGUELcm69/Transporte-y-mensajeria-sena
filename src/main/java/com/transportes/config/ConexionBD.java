package com.transportes.config;

import com.transportes.exception.ConfiguracionException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConexionBD {

    private static String url;
    private static String usuario;
    private static String password;

    private ConexionBD() {}

    public static Connection getConnection() throws SQLException {
        if (url == null) {
            cargarConfiguracion();
        }
        return DriverManager.getConnection(url, usuario, password);
    }

    private static void cargarConfiguracion() {
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

        url      = obtener("DB_URL",      props, "db.url",      "URL de la base de datos");
        usuario  = obtener("DB_USUARIO",  props, "db.usuario",  "usuario de la base de datos");
        password = obtener("DB_PASSWORD", props, "db.password", "contraseña de la base de datos");
    }

    private static String obtener(String varEntorno, Properties props,
                                  String clave, String descripcion) {
        String valor = System.getenv(varEntorno);
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
