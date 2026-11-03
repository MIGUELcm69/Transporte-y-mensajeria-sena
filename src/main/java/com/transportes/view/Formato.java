package com.transportes.view;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/** Presentación común de importes, fechas e indicadores. */
public final class Formato {

    /** dd/MM/aaaa estricto: rechaza fechas inexistentes como 31/02. */
    static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Formato() {
    }

    public static String fecha(LocalDate fecha) {
        return FECHA.format(fecha);
    }

    public static String dinero(BigDecimal importe) {
        return importe.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** Medidas sin ceros finales: 125.00 → 125, 0.500 → 0.5. */
    public static String decimal(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString();
    }

    public static String siNo(boolean valor) {
        return valor ? "sí" : "no";
    }
}
