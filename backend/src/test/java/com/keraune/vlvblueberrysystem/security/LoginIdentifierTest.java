package com.keraune.vlvblueberrysystem.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginIdentifierTest {
    @Test
    void normalizaUsuarioOCorreoParaLogin() {
        assertEquals("admin", LoginIdentifier.normalize("  Admin "));
        assertEquals("operario@vlv.agro.pe", LoginIdentifier.normalize(" OPERARIO@VLV.AGRO.PE "));
    }

    @Test
    void detectaCorreoComoIdentificadorValido() {
        assertFalse(LoginIdentifier.isEmail("admin"));
        assertTrue(LoginIdentifier.isEmail("operario@vlv.agro.pe"));
    }
}
