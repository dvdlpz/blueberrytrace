package com.keraune.vlvblueberrysystem.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RolePermissionPolicyTest {
    @Test
    void administradorMantieneGestionDeRolesYUsuarios() {
        assertTrue(RolePermissionPolicy.modulesFor(SecurityRoles.ADMINISTRADOR).contains("roles"));
        assertTrue(RolePermissionPolicy.modulesFor(SecurityRoles.ADMINISTRADOR).contains("usuarios"));
        assertTrue(RolePermissionPolicy.allows(SecurityRoles.ADMINISTRADOR, "roles", "editar_descripcion"));
    }

    @Test
    void consultaNoObtieneAccionesOperativasNiAdministrativas() {
        assertTrue(RolePermissionPolicy.modulesFor(SecurityRoles.CONSULTA).contains("trazabilidad"));
        assertFalse(RolePermissionPolicy.modulesFor(SecurityRoles.CONSULTA).contains("usuarios"));
        assertFalse(RolePermissionPolicy.allows(SecurityRoles.CONSULTA, "mermas", "crear"));
    }
}
