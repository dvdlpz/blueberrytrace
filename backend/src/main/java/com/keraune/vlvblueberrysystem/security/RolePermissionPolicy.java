package com.keraune.vlvblueberrysystem.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Defines the fixed role catalog, the action catalog and the secure defaults.
 * Administrators may configure selections stored in the database, but cannot
 * create arbitrary technical roles or unsupported actions.
 */
public final class RolePermissionPolicy {
    public record Permission(String module, String label, List<String> actions) {}

    private static final Map<String, List<Permission>> DEFAULTS = defaults();
    private static final List<Permission> CATALOG = catalog();

    private RolePermissionPolicy() {
    }

    public static List<Permission> defaultPermissionsFor(String role) {
        return DEFAULTS.getOrDefault(normalize(role), List.of());
    }

    /**
     * Returns the default visible modules for a fixed role. Runtime authorization
     * is evaluated from persisted selections by RolePermissionService.
     */
    public static Set<String> modulesFor(String role) {
        Set<String> modules = new LinkedHashSet<>();
        defaultPermissionsFor(role).forEach(permission -> {
            if (permission.actions().contains("ver")) {
                modules.add(permission.module());
            }
        });
        return Set.copyOf(modules);
    }

    /** Keeps the default policy available for validation and lightweight tests. */
    public static boolean allows(String role, String module, String action) {
        String expectedKey = key(module, action);
        return defaultPermissionsFor(role).stream()
                .flatMap(permission -> permission.actions().stream()
                        .map(availableAction -> key(permission.module(), availableAction)))
                .anyMatch(expectedKey::equals);
    }

    public static List<Permission> catalogFor(String role) {
        if (SecurityRoles.ADMINISTRADOR.equals(normalize(role))) {
            return CATALOG;
        }
        return CATALOG.stream()
                .filter(permission -> !permission.module().equals("usuarios") && !permission.module().equals("roles"))
                .toList();
    }

    public static boolean isSupportedPermission(String role, String module, String action) {
        return catalogFor(role).stream()
                .anyMatch(permission -> permission.module().equals(normalizeModule(module))
                        && permission.actions().contains(normalizeAction(action)));
    }

    public static Set<String> mandatorySelectionsFor(String role) {
        Set<String> required = new LinkedHashSet<>();
        required.add(key("dashboard", "ver"));
        if (SecurityRoles.ADMINISTRADOR.equals(normalize(role))) {
            required.add(key("usuarios", "ver"));
            required.add(key("usuarios", "crear"));
            required.add(key("usuarios", "editar"));
            required.add(key("usuarios", "activar"));
            required.add(key("usuarios", "restablecer_contrasena"));
            required.add(key("roles", "ver"));
            required.add(key("roles", "editar_descripcion"));
            required.add(key("roles", "editar_permisos"));
            required.add(key("roles", "cambiar_estado"));
        }
        return Set.copyOf(required);
    }

    public static String key(String module, String action) {
        return normalizeModule(module) + "." + normalizeAction(action);
    }

    public static String normalizeModule(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeAction(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, List<Permission>> defaults() {
        Map<String, List<Permission>> values = new LinkedHashMap<>();
        values.put(SecurityRoles.ADMINISTRADOR, List.of(
                permission("dashboard", "Panel operativo", "ver"),
                permission("usuarios", "Usuarios", "ver", "crear", "editar", "activar", "restablecer_contrasena"),
                permission("roles", "Roles y permisos", "ver", "editar_descripcion", "editar_permisos", "cambiar_estado"),
                permission("lotes", "Lotes e invernaderos", "ver", "crear", "editar", "cambiar_estado", "archivar"),
                permission("camas", "Camas", "ver", "crear", "editar", "cambiar_estado"),
                permission("jabas", "Jabas de siembra", "ver", "crear", "editar", "cambiar_estado"),
                permission("lotes_trazables", "Lotes trazables", "ver", "crear", "editar", "cambiar_estado", "normalizar_legado"),
                permission("siembra", "Siembra", "ver", "crear", "editar", "anular"),
                permission("riegos", "Riegos programados", "ver", "programar", "realizar", "cancelar"),
                permission("procesos", "Procesos", "ver", "crear", "editar", "anular"),
                permission("recuperacion", "Recuperación por riego", "ver", "crear", "cerrar"),
                permission("clasificacion", "Clasificación", "ver", "crear", "editar", "validar", "observar"),
                permission("pedidos", "Pedidos por variedad", "ver", "crear", "editar", "cambiar_estado"),
                permission("empaques", "Empaques", "ver", "crear", "anular"),
                permission("despacho", "Despacho", "ver", "crear", "editar", "cambiar_estado"),
                permission("mermas", "Mermas y ajustes", "ver", "crear", "anular"),
                permission("trazabilidad", "Trazabilidad", "ver"),
                permission("reportes", "Reportes técnicos", "ver", "exportar"),
                permission("auditoria", "Auditoría", "ver", "exportar")
        ));
        values.put(SecurityRoles.SUPERVISOR, List.of(
                permission("dashboard", "Panel operativo", "ver"),
                permission("lotes", "Lotes e invernaderos", "ver", "crear", "editar", "cambiar_estado", "archivar"),
                permission("camas", "Camas", "ver", "crear", "editar", "cambiar_estado"),
                permission("jabas", "Jabas de siembra", "ver", "crear", "editar", "cambiar_estado"),
                permission("lotes_trazables", "Lotes trazables", "ver", "crear", "editar", "cambiar_estado"),
                permission("siembra", "Siembra", "ver", "crear", "editar", "anular"),
                permission("riegos", "Riegos programados", "ver", "programar", "realizar", "cancelar"),
                permission("procesos", "Procesos", "ver", "crear", "editar", "anular"),
                permission("recuperacion", "Recuperación por riego", "ver", "crear", "cerrar"),
                permission("clasificacion", "Clasificación", "ver", "crear", "editar", "validar", "observar"),
                permission("pedidos", "Pedidos por variedad", "ver", "crear", "editar", "cambiar_estado"),
                permission("empaques", "Empaques", "ver", "crear", "anular"),
                permission("despacho", "Despacho", "ver", "crear", "editar", "cambiar_estado"),
                permission("mermas", "Mermas y ajustes", "ver", "crear", "anular"),
                permission("trazabilidad", "Trazabilidad", "ver"),
                permission("reportes", "Reportes técnicos", "ver", "exportar"),
                permission("auditoria", "Auditoría", "ver", "exportar")
        ));
        values.put(SecurityRoles.OPERARIO, List.of(
                permission("dashboard", "Panel operativo", "ver"),
                permission("jabas", "Jabas de siembra", "ver"),
                permission("lotes_trazables", "Lotes trazables", "ver"),
                permission("siembra", "Siembra", "ver", "crear", "editar", "anular"),
                permission("riegos", "Riegos programados", "ver", "programar", "realizar"),
                permission("procesos", "Procesos", "ver", "crear", "editar", "anular"),
                permission("recuperacion", "Recuperación por riego", "ver", "crear", "cerrar"),
                permission("mermas", "Mermas y ajustes", "ver", "crear"),
                permission("trazabilidad", "Trazabilidad", "ver"),
                permission("reportes", "Reportes técnicos", "ver", "exportar")
        ));
        values.put(SecurityRoles.CONTROL_CALIDAD, List.of(
                permission("dashboard", "Panel operativo", "ver"),
                permission("jabas", "Jabas de siembra", "ver"),
                permission("lotes_trazables", "Lotes trazables", "ver"),
                permission("riegos", "Riegos programados", "ver"),
                permission("recuperacion", "Recuperación por riego", "ver", "crear", "cerrar"),
                permission("clasificacion", "Clasificación", "ver", "crear", "editar", "validar", "observar"),
                permission("pedidos", "Pedidos por variedad", "ver", "crear", "editar", "cambiar_estado"),
                permission("empaques", "Empaques", "ver", "crear", "anular"),
                permission("despacho", "Despacho", "ver", "crear", "editar", "cambiar_estado"),
                permission("mermas", "Mermas y ajustes", "ver", "crear"),
                permission("trazabilidad", "Trazabilidad", "ver"),
                permission("reportes", "Reportes técnicos", "ver", "exportar")
        ));
        values.put(SecurityRoles.CONSULTA, List.of(
                permission("dashboard", "Panel operativo", "ver"),
                permission("lotes_trazables", "Lotes trazables", "ver"),
                permission("trazabilidad", "Trazabilidad", "ver"),
                permission("reportes", "Reportes técnicos", "ver", "exportar")
        ));
        return Map.copyOf(values);
    }

    private static List<Permission> catalog() {
        Map<String, Permission> byModule = new LinkedHashMap<>();
        for (Permission permission : defaultPermissionsFor(SecurityRoles.ADMINISTRADOR)) {
            byModule.put(permission.module(), permission);
        }
        return List.copyOf(new ArrayList<>(byModule.values()));
    }

    private static Permission permission(String module, String label, String... actions) {
        return new Permission(module, label, List.of(actions));
    }

    private static String normalize(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }
}
