package com.transportes.config;

import com.transportes.exception.ConfiguracionException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ConexionBDTest {

    private static Properties archivo(Map<String, String> claves) {
        Properties props = new Properties();
        props.putAll(claves);
        return props;
    }

    @Test
    void conConfiguracionIncompletaLanzaConfiguracionExceptionCadaVezSinConectar() {
        ConexionBD conexion = new ConexionBD(
                variable -> null,
                () -> archivo(Map.of("db.url", "jdbc:mysql://127.0.0.1:3306/transportes_db",
                        "db.usuario", "transportes_app")),
                (url, usuario, password) -> fail("no debe intentar conectar sin configuración completa"));

        ConfiguracionException primera = assertThrows(ConfiguracionException.class, conexion::abrir);
        ConfiguracionException segunda = assertThrows(ConfiguracionException.class, conexion::abrir);

        assertEquals(primera.getMessage(), segunda.getMessage());
        assertTrue(segunda.getMessage().contains("DB_PASSWORD"), segunda.getMessage());
    }

    @Test
    void conConfiguracionCompletaConectaConLosTresValoresYLasVariablesDeEntornoTienenPrioridad()
            throws SQLException {
        List<String> recibidos = new ArrayList<>();
        ConexionBD conexion = new ConexionBD(
                variable -> variable.equals("DB_USUARIO") ? "usuario_entorno" : null,
                () -> archivo(Map.of("db.url", " jdbc:mysql://127.0.0.1:3306/transportes_db ",
                        "db.usuario", "usuario_archivo", "db.password", "clave")),
                (url, usuario, password) -> {
                    recibidos.addAll(List.of(url, usuario, password));
                    return null;
                });

        conexion.abrir();

        assertEquals(List.of("jdbc:mysql://127.0.0.1:3306/transportes_db", "usuario_entorno", "clave"), recibidos);
    }
}
