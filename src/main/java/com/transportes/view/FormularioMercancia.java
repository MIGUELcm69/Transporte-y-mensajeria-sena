package com.transportes.view;

import com.transportes.model.Mercancia;
import com.transportes.model.Paquete;
import com.transportes.model.Sobre;
import com.transportes.model.TipoMercancia;
import com.transportes.model.TipoVehiculo;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * Preguntas y presentación de una mercancía para MercanciaView y ReportesView.
 * Los switch sobre TipoMercancia no tienen default: un tipo nuevo obliga a completar cada formulario.
 */
final class FormularioMercancia {

    /** Muestra los vehículos del tipo pedido y devuelve el id del elegido; {@code actualId} null = obligatorio. */
    @FunctionalInterface
    interface SelectorVehiculo {
        int elegir(String etiqueta, TipoVehiculo tipo, Integer actualId);
    }

    private record Tramos(int vehiculoId, Integer avionId, Integer vehiculoDestinoId) {
    }

    private final Consola consola;
    private final SelectorVehiculo selector;

    FormularioMercancia(Consola consola, SelectorVehiculo selector) {
        this.consola = Objects.requireNonNull(consola, "consola");
        this.selector = Objects.requireNonNull(selector, "selector");
    }

    TipoMercancia elegirTipo() {
        List<TipoMercancia> tipos = Arrays.asList(TipoMercancia.values());
        int elegido = consola.elegirOpcion("Tipo de mercancía", tipos.stream().map(Enum::name).toList(), "Cancelar");
        if (elegido == 0) {
            throw new OperacionCancelada();
        }
        return tipos.get(elegido - 1);
    }

    /** Pide los datos de una mercancía nueva; cada llamada crea un objeto nuevo y sin id. */
    Mercancia pedirNueva(TipoMercancia tipo) {
        String codigo = consola.leerTextoNoVacio("Código");
        String descripcion = consola.leerTextoNoVacio("Descripción");
        BigDecimal precioEnvio = consola.leerDecimal("Precio de envío", Consola.Rango.NO_NEGATIVO);
        boolean asegurada = consola.confirmar("¿Está asegurada?");
        boolean largoRecorrido = consola.confirmar("¿Es de largo recorrido?");
        return switch (tipo) {
            case SOBRE -> {
                BigDecimal peso = consola.leerDecimal("Peso", Consola.Rango.POSITIVO);
                Tramos tramos = pedirTramos(tipo, largoRecorrido, null);
                yield new Sobre(null, codigo, descripcion, precioEnvio, asegurada, largoRecorrido,
                        tramos.vehiculoId(), tramos.avionId(), tramos.vehiculoDestinoId(), peso);
            }
            case PAQUETE -> {
                BigDecimal largo = consola.leerDecimal("Largo", Consola.Rango.POSITIVO);
                BigDecimal ancho = consola.leerDecimal("Ancho", Consola.Rango.POSITIVO);
                BigDecimal alto = consola.leerDecimal("Alto", Consola.Rango.POSITIVO);
                Tramos tramos = pedirTramos(tipo, largoRecorrido, null);
                yield new Paquete(null, codigo, descripcion, precioEnvio, asegurada, largoRecorrido,
                        tramos.vehiculoId(), tramos.avionId(), tramos.vehiculoDestinoId(), largo, ancho, alto);
            }
        };
    }

    /**
     * Pide los datos modificables (Enter conserva cada uno) y devuelve un objeto nuevo con el mismo id y código.
     * Si el precio de envío no es modificable, no se pregunta y se conserva.
     */
    Mercancia pedirModificada(Mercancia actual, boolean precioModificable) {
        String descripcion = consola.leerTextoOActual("Descripción", actual.getDescripcion());
        BigDecimal precioEnvio = precioModificable
                ? consola.leerDecimalOActual("Precio de envío", Consola.Rango.NO_NEGATIVO, actual.getPrecioEnvio())
                : actual.getPrecioEnvio();
        boolean asegurada = consola.confirmarOActual("¿Está asegurada?", actual.isAsegurada());
        boolean largoRecorrido = consola.confirmarOActual("¿Es de largo recorrido?", actual.isLargoRecorrido());
        return switch (actual.getTipo()) {
            case SOBRE -> {
                BigDecimal peso = consola.leerDecimalOActual("Peso", Consola.Rango.POSITIVO,
                        ((Sobre) actual).getPeso());
                Tramos tramos = pedirTramos(actual.getTipo(), largoRecorrido, actual);
                yield new Sobre(actual.getId(), actual.getCodigo(), descripcion, precioEnvio, asegurada,
                        largoRecorrido, tramos.vehiculoId(), tramos.avionId(), tramos.vehiculoDestinoId(), peso);
            }
            case PAQUETE -> {
                Paquete paquete = (Paquete) actual;
                BigDecimal largo = consola.leerDecimalOActual("Largo", Consola.Rango.POSITIVO, paquete.getLargo());
                BigDecimal ancho = consola.leerDecimalOActual("Ancho", Consola.Rango.POSITIVO, paquete.getAncho());
                BigDecimal alto = consola.leerDecimalOActual("Alto", Consola.Rango.POSITIVO, paquete.getAlto());
                Tramos tramos = pedirTramos(actual.getTipo(), largoRecorrido, actual);
                yield new Paquete(actual.getId(), actual.getCodigo(), descripcion, precioEnvio, asegurada,
                        largoRecorrido, tramos.vehiculoId(), tramos.avionId(), tramos.vehiculoDestinoId(),
                        largo, ancho, alto);
            }
        };
    }

    /** Sin largo recorrido solo hay vehículo; con él, además avión y vehículo de destino (RF-21, RF-23, RF-24). */
    private Tramos pedirTramos(TipoMercancia tipo, boolean largoRecorrido, Mercancia actual) {
        TipoVehiculo terrestre = tipo.vehiculoTerrestre();
        int vehiculoId = selector.elegir("Vehículo", terrestre, actual == null ? null : actual.getVehiculoId());
        if (!largoRecorrido) {
            return new Tramos(vehiculoId, null, null);
        }
        int avionId = selector.elegir("Avión", TipoVehiculo.AVION, actual == null ? null : actual.getAvionId());
        int destinoId = selector.elegir("Vehículo de destino", terrestre,
                actual == null ? null : actual.getVehiculoDestinoId());
        return new Tramos(vehiculoId, avionId, destinoId);
    }

    /** Una línea: código, tipo, descripción, precio, indicadores y las medidas propias del tipo. */
    static String resumen(Mercancia mercancia) {
        String linea = mercancia.getCodigo() + " · " + mercancia.getTipo() + " · " + mercancia.getDescripcion()
                + " · precio " + Formato.dinero(mercancia.getPrecioEnvio())
                + " · asegurada " + Formato.siNo(mercancia.isAsegurada())
                + " · largo recorrido " + Formato.siNo(mercancia.isLargoRecorrido());
        return switch (mercancia.getTipo()) {
            case SOBRE -> linea + " · peso " + Formato.decimal(((Sobre) mercancia).getPeso());
            case PAQUETE -> {
                Paquete paquete = (Paquete) mercancia;
                yield linea + " · medidas " + Formato.decimal(paquete.getLargo()) + " x "
                        + Formato.decimal(paquete.getAncho()) + " x " + Formato.decimal(paquete.getAlto());
            }
        };
    }

    /** Códigos de los vehículos asignados: vehículo y, si es de largo recorrido, avión y vehículo de destino. */
    static String tramos(Mercancia mercancia, IntFunction<String> codigoVehiculo) {
        String linea = "vehículo " + codigoVehiculo.apply(mercancia.getVehiculoId());
        if (!mercancia.isLargoRecorrido()) {
            return linea;
        }
        return linea + " · avión " + codigoVehiculo.apply(mercancia.getAvionId())
                + " · vehículo de destino " + codigoVehiculo.apply(mercancia.getVehiculoDestinoId());
    }
}
