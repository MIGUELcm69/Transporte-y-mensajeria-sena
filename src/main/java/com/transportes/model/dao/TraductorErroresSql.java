package com.transportes.model.dao;

import com.transportes.exception.AccesoDatosException;
import com.transportes.exception.ClienteConEnviosException;
import com.transportes.exception.DatoInvalidoException;
import com.transportes.exception.DomainException;
import com.transportes.exception.MercanciaConEnvioException;
import com.transportes.exception.RegistroDuplicadoException;
import com.transportes.exception.ReglaAsignacionException;
import com.transportes.exception.SupervisorConVehiculosException;
import com.transportes.exception.VehiculoEnUsoException;

import java.sql.SQLException;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte una SQLException en una excepción con mensaje apto para el usuario.
 * El mensaje original de MySQL solo va al log; al usuario llega el texto de un trigger (1644),
 * que está redactado para él en sql/schema.sql, o un mensaje propio.
 */
final class TraductorErroresSql {

    private static final Logger LOG = Logger.getLogger(TraductorErroresSql.class.getName());

    private static final int DUPLICADO = 1062;
    private static final int FILA_PADRE_EN_USO = 1451;
    private static final int REFERENCIA_INEXISTENTE = 1452;
    private static final int FUERA_DE_RANGO = 1264;
    private static final int SIGNAL_TRIGGER = 1644;
    private static final int CHECK_VIOLADO = 3819;

    // "Duplicate entry 'x' for key 'tabla.uq_nombre'": se toma la última coincidencia porque el valor puede contener comillas.
    private static final Pattern CLAVE_DUPLICADA = Pattern.compile("for key '(?:[^'.]*\\.)?([^'.]+)'");
    private static final Pattern CHECK = Pattern.compile("[Cc]heck constraint '([^']+)'");
    private static final Pattern CLAVE_FORANEA = Pattern.compile("CONSTRAINT `([^`]+)`");

    private static final Map<String, String> DUPLICADOS = Map.of(
            "uq_supervisor_nif", "Ya existe un supervisor con ese NIF.",
            "uq_vehiculo_codigo", "Ya existe un vehículo con ese código.",
            "uq_cliente_codigo", "Ya existe un cliente con ese código.",
            "uq_cliente_nif", "Ya existe un cliente con ese NIF.",
            "uq_cliente_cif", "Ya existe un cliente con ese CIF.",
            "uq_mercancia_codigo", "Ya existe una mercancía con ese código.",
            "uq_envio_mercancia", "La mercancía ya tiene un envío registrado.");

    private static final Map<String, String> CHECKS = Map.ofEntries(
            Map.entry("chk_vehiculo_precio", "El precio de compra no puede ser negativo."),
            Map.entry("chk_vehiculo_subtipo",
                    "Los datos del vehículo no corresponden a su tipo (cilindrada para moto, capacidad para furgoneta)."),
            Map.entry("chk_cliente_facturado", "El total facturado del cliente no puede quedar negativo."),
            Map.entry("chk_cliente_subtipo",
                    "Los datos del cliente no corresponden a su tipo (NIF para particular; CIF y teléfono para empresa)."),
            Map.entry("chk_mercancia_precio", "El precio de envío no puede ser negativo."),
            Map.entry("chk_mercancia_subtipo",
                    "Los datos de la mercancía no corresponden a su tipo (peso para sobre; largo, ancho y alto para paquete)."),
            Map.entry("chk_mercancia_largo_recorrido",
                    "Una mercancía de largo recorrido requiere avión y vehículo de destino, y solo ella puede tenerlos."),
            Map.entry("chk_envio_intentos", "Los intentos fallidos no pueden ser negativos."),
            Map.entry("chk_envio_precio", "El precio total del envío no puede ser negativo."),
            Map.entry("chk_envio_clientes", "El cliente origen y el destinatario deben ser distintos."));

    // 1451: la FK violada indica qué entidad se intentó borrar y, por tanto, qué excepción "en uso" corresponde.
    private static final Map<String, Supplier<DomainException>> EN_USO = Map.of(
            "fk_vehiculo_supervisor", () -> new SupervisorConVehiculosException(
                    "No se puede eliminar el supervisor porque tiene vehículos a cargo."),
            "fk_mercancia_vehiculo", () -> new VehiculoEnUsoException(
                    "No se puede eliminar el vehículo porque tiene mercancías asignadas."),
            "fk_mercancia_avion", () -> new VehiculoEnUsoException(
                    "No se puede eliminar el avión porque tiene mercancías asignadas."),
            "fk_mercancia_destino", () -> new VehiculoEnUsoException(
                    "No se puede eliminar el vehículo porque es el de destino de alguna mercancía."),
            "fk_envio_mercancia", () -> new MercanciaConEnvioException(
                    "No se puede eliminar la mercancía porque tiene un envío asociado."),
            "fk_envio_origen", () -> new ClienteConEnviosException(
                    "No se puede eliminar el cliente porque tiene envíos encargados."),
            "fk_envio_destinatario", () -> new ClienteConEnviosException(
                    "No se puede eliminar el cliente porque es destinatario de algún envío."));

    private TraductorErroresSql() {
    }

    static RuntimeException traducir(SQLException e, String operacion) {
        String detalle = e.getMessage() == null ? "" : e.getMessage();
        RuntimeException traducida = switch (e.getErrorCode()) {
            case DUPLICADO -> new RegistroDuplicadoException(
                    DUPLICADOS.getOrDefault(restriccion(CLAVE_DUPLICADA, detalle), "Ya existe un registro con esos datos."));
            case CHECK_VIOLADO -> new DatoInvalidoException(
                    CHECKS.getOrDefault(restriccion(CHECK, detalle), "Los datos introducidos no son válidos."));
            case SIGNAL_TRIGGER -> new ReglaAsignacionException(detalle);
            case FILA_PADRE_EN_USO -> enUso(restriccion(CLAVE_FORANEA, detalle));
            case REFERENCIA_INEXISTENTE -> new DatoInvalidoException("Se hace referencia a un registro que no existe.");
            case FUERA_DE_RANGO -> new DatoInvalidoException("Valor fuera de rango.");
            default -> new AccesoDatosException("No se pudo completar la operación en la base de datos.", e);
        };
        Level nivel = traducida instanceof AccesoDatosException ? Level.SEVERE : Level.WARNING;
        LOG.log(nivel, "Error SQL al " + operacion + " [código " + e.getErrorCode()
                + ", SQLState " + e.getSQLState() + "]", e);
        return traducida;
    }

    private static DomainException enUso(String restriccion) {
        Supplier<DomainException> fabrica = EN_USO.get(restriccion);
        return fabrica != null
                ? fabrica.get()
                : new DomainException("No se puede eliminar el registro porque está en uso.");
    }

    private static String restriccion(Pattern patron, String detalle) {
        Matcher matcher = patron.matcher(detalle);
        String nombre = "";
        while (matcher.find()) {
            nombre = matcher.group(1);
        }
        return nombre;
    }
}
