package com.transportes.model;

import com.transportes.exception.DatoInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentoTest {

    private static final String NIT_ONCE_CARACTERES = "900123456-7";
    private static final String DOCUMENTO_VEINTIUN_CARACTERES = "123456789012345678901";

    private static ClienteEmpresa empresaConCif(String cif) {
        return new ClienteEmpresa(null, "CLI-100", LocalDate.of(2026, 1, 15), BigDecimal.ZERO,
                cif, "Empresa S.A.S.", "Calle 10 # 20-30", "6011234567");
    }

    @Test
    void documentoDeOnceCaracteresSeAcepta() {
        Supervisor supervisor = new Supervisor(null, NIT_ONCE_CARACTERES, "Ana Pérez", "Calle 1", "3001234567");

        assertEquals(NIT_ONCE_CARACTERES, supervisor.getNif());
        assertEquals(NIT_ONCE_CARACTERES, empresaConCif(NIT_ONCE_CARACTERES).getCif());
    }

    @Test
    void documentoDeVeintiunCaracteresSeRechaza() {
        assertThrows(DatoInvalidoException.class,
                () -> new Supervisor(null, DOCUMENTO_VEINTIUN_CARACTERES, "Ana Pérez", "Calle 1", "3001234567"));
        assertThrows(DatoInvalidoException.class, () -> empresaConCif(DOCUMENTO_VEINTIUN_CARACTERES));
    }
}
