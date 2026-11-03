package com.transportes.view;

import com.transportes.model.Avion;
import com.transportes.model.Furgoneta;
import com.transportes.model.Moto;
import com.transportes.model.TipoVehiculo;
import com.transportes.model.Vehiculo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * Preguntas y presentación de un vehículo, compartidas por VehiculoView y SupervisorView.
 * Los switch sobre TipoVehiculo no tienen default: un tipo nuevo obliga a completar cada formulario.
 */
final class FormularioVehiculo {

    private final Consola consola;

    FormularioVehiculo(Consola consola) {
        this.consola = Objects.requireNonNull(consola, "consola");
    }

    TipoVehiculo elegirTipo() {
        List<TipoVehiculo> tipos = Arrays.asList(TipoVehiculo.values());
        int elegido = consola.elegirOpcion("Tipo de vehículo", tipos.stream().map(Enum::name).toList(), "Cancelar");
        if (elegido == 0) {
            throw new OperacionCancelada();
        }
        return tipos.get(elegido - 1);
    }

    /**
     * Pide los datos de un vehículo nuevo y devuelve cómo construirlo para un supervisor. Cada llamada a la
     * función crea un objeto nuevo, así que un reintento tras un rollback nunca reutiliza un vehículo con id.
     */
    IntFunction<Vehiculo> pedirNuevo(TipoVehiculo tipo) {
        String codigo = consola.leerTextoNoVacio("Código");
        String descripcion = consola.leerTextoNoVacio("Descripción");
        LocalDate fechaCompra = consola.leerFecha("Fecha de compra");
        BigDecimal precioCompra = consola.leerDecimal("Precio de compra", Consola.Rango.NO_NEGATIVO);
        return switch (tipo) {
            case MOTO -> {
                BigDecimal cilindrada = consola.leerDecimal("Cilindrada", Consola.Rango.POSITIVO);
                yield supervisorId -> new Moto(null, codigo, descripcion, fechaCompra, precioCompra, supervisorId,
                        cilindrada);
            }
            case FURGONETA -> {
                BigDecimal capacidad = consola.leerDecimal("Capacidad", Consola.Rango.POSITIVO);
                yield supervisorId -> new Furgoneta(null, codigo, descripcion, fechaCompra, precioCompra,
                        supervisorId, capacidad);
            }
            case AVION -> {
                yield supervisorId -> new Avion(null, codigo, descripcion, fechaCompra, precioCompra, supervisorId);
            }
        };
    }

    /** Pide los datos modificables (Enter conserva cada uno) y devuelve un objeto nuevo con el mismo id y código. */
    Vehiculo pedirModificado(Vehiculo actual, int supervisorId) {
        String descripcion = consola.leerTextoOActual("Descripción", actual.getDescripcion());
        LocalDate fechaCompra = consola.leerFechaOActual("Fecha de compra", actual.getFechaCompra());
        BigDecimal precioCompra = consola.leerDecimalOActual("Precio de compra", Consola.Rango.NO_NEGATIVO,
                actual.getPrecioCompra());
        return switch (actual.getTipo()) {
            case MOTO -> new Moto(actual.getId(), actual.getCodigo(), descripcion, fechaCompra, precioCompra,
                    supervisorId, consola.leerDecimalOActual("Cilindrada", Consola.Rango.POSITIVO,
                            ((Moto) actual).getCilindrada()));
            case FURGONETA -> new Furgoneta(actual.getId(), actual.getCodigo(), descripcion, fechaCompra,
                    precioCompra, supervisorId, consola.leerDecimalOActual("Capacidad", Consola.Rango.POSITIVO,
                            ((Furgoneta) actual).getCapacidad()));
            case AVION -> new Avion(actual.getId(), actual.getCodigo(), descripcion, fechaCompra, precioCompra,
                    supervisorId);
        };
    }

    /** Una línea: código, tipo, descripción, compra, precio y el dato propio del tipo. */
    static String resumen(Vehiculo vehiculo) {
        String linea = vehiculo.getCodigo() + " · " + vehiculo.getTipo() + " · " + vehiculo.getDescripcion()
                + " · compra " + Formato.fecha(vehiculo.getFechaCompra())
                + " · precio " + Formato.dinero(vehiculo.getPrecioCompra());
        return switch (vehiculo.getTipo()) {
            case MOTO -> linea + " · cilindrada " + Formato.decimal(((Moto) vehiculo).getCilindrada());
            case FURGONETA -> linea + " · capacidad " + Formato.decimal(((Furgoneta) vehiculo).getCapacidad());
            case AVION -> linea;
        };
    }
}
