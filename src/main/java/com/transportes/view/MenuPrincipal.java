package com.transportes.view;

import java.util.List;
import java.util.Objects;

/** RF-38. */
public class MenuPrincipal {

    private final Consola consola;
    private final ManejadorErrores manejador;
    private final List<VistaModulo> modulos;

    public MenuPrincipal(Consola consola, ManejadorErrores manejador, List<VistaModulo> modulos) {
        this.consola = Objects.requireNonNull(consola, "consola");
        this.manejador = Objects.requireNonNull(manejador, "manejador");
        this.modulos = List.copyOf(modulos);
    }

    public void iniciar() {
        consola.mostrar("Sistema de gestión de transportes y mensajería");
        consola.mostrar("En cualquier campo, escriba '" + Consola.CANCELAR + "' para abandonar la operación.");
        List<String> textos = modulos.stream().map(VistaModulo::titulo).toList();
        try {
            while (true) {
                int elegida = consola.elegirOpcion("Menú principal", textos, "Salir");
                if (elegida == 0) {
                    break;
                }
                // Última red: si algo escapa del bucle de un módulo, se vuelve aquí en vez de cerrar la aplicación.
                manejador.ejecutar(modulos.get(elegida - 1)::abrir);
            }
        } catch (FinDeEntrada e) {
            consola.mostrar("");
        }
        consola.mostrar("Hasta pronto.");
    }
}
