package com.transportes.view;

/** La entrada se agotó (Ctrl+D o fin de un flujo de prueba): la aplicación termina sin quedarse en bucle. */
public class FinDeEntrada extends RuntimeException {

    public FinDeEntrada() {
        super("No hay más entrada.");
    }
}
