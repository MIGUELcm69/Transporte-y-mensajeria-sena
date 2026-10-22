package com.transportes;

import com.transportes.config.ConexionBD;
import com.transportes.exception.ConfiguracionException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM vehiculo");
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            int total = rs.getInt(1);
            System.out.println("Conexión exitosa. Vehículos registrados: " + total);

        } catch (ConfiguracionException e) {
            System.err.println("Error de configuración: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("No se pudo conectar con la base de datos. " +
                    "Verifique que el servidor MySQL esté activo y que la " +
                    "configuración en config.properties sea correcta.");
        }
    }
}
