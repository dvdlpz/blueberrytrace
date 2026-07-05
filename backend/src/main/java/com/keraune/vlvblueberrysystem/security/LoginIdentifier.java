package com.keraune.vlvblueberrysystem.security;

/**
 * Utilidad pequeña para normalizar el identificador de acceso.
 * Permite que el mismo flujo de autenticación acepte username o correo electrónico.
 */
public final class LoginIdentifier {
    private LoginIdentifier() {
    }

    public static String normalize(String identifier) {
        if (identifier == null) {
            return "";
        }
        return identifier.trim().toLowerCase();
    }

    public static boolean isEmail(String identifier) {
        return normalize(identifier).contains("@");
    }
}
