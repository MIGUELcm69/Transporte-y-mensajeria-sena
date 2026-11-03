package com.transportes.view;

import java.util.List;
import java.util.Objects;

/** Bucle común de los submenús: cada opción se ejecuta dentro del ManejadorErrores. */
public abstract class VistaModulo {

    public record Opcion(String texto, Runnable accion) {

        public Opcion {
            Objects.requireNonNull(texto, "texto");
            Objects.requireNonNull(accion, "accion");
        }
    }

    protected final Consola consola;
    private final ManejadorErrores manejador;

    protected VistaModulo(Consola consola, ManejadorErrores manejador) {
        this.consola = Objects.requireNonNull(consola, "consola");
        this.manejador = Objects.requireNonNull(manejador, "manejador");
    }

    public abstract String titulo();

    /** Cada acción construye sus objetos de dominio al ejecutarse, nunca los reutiliza entre intentos. */
    protected abstract List<Opcion> opciones();

    public void abrir() {
        List<Opcion> opciones = opciones();
        List<String> textos = opciones.stream().map(Opcion::texto).toList();
        while (true) {
            int elegida = consola.elegirOpcion(titulo(), textos, "Volver");
            if (elegida == 0) {
                return;
            }
            manejador.ejecutar(opciones.get(elegida - 1).accion());
        }
    }
}
