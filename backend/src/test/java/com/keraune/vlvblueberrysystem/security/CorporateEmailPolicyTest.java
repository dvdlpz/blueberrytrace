package com.keraune.vlvblueberrysystem.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorporateEmailPolicyTest {
    @Test
    void aceptaDominiosConfigurados() {
        CorporateEmailPolicy policy = new CorporateEmailPolicy("vlv.agro.pe, viverolosvinedos.pe");
        assertDoesNotThrow(() -> policy.validate("operario@vlv.agro.pe"));
        assertDoesNotThrow(() -> policy.validate("control@viverolosvinedos.pe"));
    }

    @Test
    void rechazaDominioFueraDeLaPolitica() {
        CorporateEmailPolicy policy = new CorporateEmailPolicy("vlv.agro.pe");
        assertThrows(IllegalArgumentException.class, () -> policy.validate("persona@externo.pe"));
    }
}
