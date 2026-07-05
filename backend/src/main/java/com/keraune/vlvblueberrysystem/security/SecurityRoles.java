package com.keraune.vlvblueberrysystem.security;

import java.util.List;
import java.util.Locale;

public final class SecurityRoles {
    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String SUPERVISOR = "SUPERVISOR";
    public static final String OPERARIO = "OPERARIO";
    public static final String CONTROL_CALIDAD = "CONTROL_CALIDAD";
    public static final String CONSULTA = "CONSULTA";

    private SecurityRoles() {
    }

    public static List<String> all() {
        return List.of(ADMINISTRADOR, SUPERVISOR, OPERARIO, CONTROL_CALIDAD, CONSULTA);
    }

    public static String[] allArray() {
        return all().toArray(String[]::new);
    }

    public static boolean isSupported(String role) {
        return role != null && all().contains(role.trim().toUpperCase(Locale.ROOT));
    }
}
