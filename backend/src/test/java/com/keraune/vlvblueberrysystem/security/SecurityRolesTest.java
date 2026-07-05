package com.keraune.vlvblueberrysystem.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityRolesTest {
    @Test
    void reconoceSoloLosRolesOperativosPermitidos() {
        assertTrue(SecurityRoles.isSupported("administrador"));
        assertTrue(SecurityRoles.isSupported(SecurityRoles.CONSULTA));
        assertFalse(SecurityRoles.isSupported("CONTABILIZADOR"));
        assertFalse(SecurityRoles.isSupported(null));
    }
}
