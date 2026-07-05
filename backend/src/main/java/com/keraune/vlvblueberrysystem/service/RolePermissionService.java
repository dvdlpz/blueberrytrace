package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.AdminPayloads.RolePermissionSelectionPayload;
import com.keraune.vlvblueberrysystem.entity.Role;
import com.keraune.vlvblueberrysystem.entity.RolePermission;
import com.keraune.vlvblueberrysystem.entity.User;
import com.keraune.vlvblueberrysystem.repository.RolePermissionRepository;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import com.keraune.vlvblueberrysystem.security.RolePermissionPolicy;
import com.keraune.vlvblueberrysystem.security.SecurityRoles;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class RolePermissionService {
    private static final String UPGRADE_MODULE = "sistema";
    private static final String UPGRADE_ACTION = "operacion_v5";

    private final RolePermissionRepository permissions;
    private final UserRepository users;

    public RolePermissionService(RolePermissionRepository permissions, UserRepository users) {
        this.permissions = permissions;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<RolePermissionPolicy.Permission> permissionsFor(Role role) {
        if (role == null || role.getId() == null) {
            return List.of();
        }
        Set<String> selected = selectedKeys(role);
        if (selected.isEmpty()) {
            return RolePermissionPolicy.defaultPermissionsFor(role.getNombre());
        }
        return grouped(role.getNombre(), selected);
    }

    @Transactional(readOnly = true)
    public Set<String> modulesFor(Role role) {
        Set<String> modules = new LinkedHashSet<>();
        permissionsFor(role).forEach(permission -> {
            if (permission.actions().contains("ver")) {
                modules.add(permission.module());
            }
        });
        return Set.copyOf(modules);
    }

    @Transactional(readOnly = true)
    public Set<String> selectionKeysFor(Role role) {
        Set<String> selected = selectedKeys(role);
        if (selected.isEmpty()) {
            selected = keys(RolePermissionPolicy.defaultPermissionsFor(role.getNombre()));
        }
        return Set.copyOf(selected);
    }

    public void ensureDefaults(Role role) {
        if (role == null || role.getId() == null) {
            return;
        }
        List<RolePermission> stored = permissions.findByRoleIdOrderByModuleKeyAscActionKeyAsc(role.getId());
        if (stored.isEmpty()) {
            permissions.saveAll(records(role, keys(RolePermissionPolicy.defaultPermissionsFor(role.getNombre()))));
            return;
        }
        boolean upgraded = stored.stream().anyMatch(item -> UPGRADE_MODULE.equals(item.getModuleKey()) && UPGRADE_ACTION.equals(item.getActionKey()));
        if (upgraded) {
            return;
        }
        Set<String> merged = new LinkedHashSet<>();
        stored.stream()
                .filter(item -> !UPGRADE_MODULE.equals(item.getModuleKey()))
                .map(item -> RolePermissionPolicy.key(item.getModuleKey(), item.getActionKey()))
                .forEach(merged::add);
        keys(RolePermissionPolicy.defaultPermissionsFor(role.getNombre())).stream()
                .filter(key -> key.startsWith("jabas.") || key.startsWith("riegos.") || key.startsWith("recuperacion.") || key.startsWith("pedidos.") || key.startsWith("empaques."))
                .forEach(merged::add);
        permissions.deleteByRoleId(role.getId());
        permissions.flush();
        List<RolePermission> migrated = new ArrayList<>(records(role, merged));
        migrated.add(record(role, UPGRADE_MODULE, UPGRADE_ACTION));
        permissions.saveAll(migrated);
    }

    /** Returns true when the stored selection changes and affected sessions are invalidated. */
    public boolean replace(Role role, List<RolePermissionSelectionPayload> incoming) {
        if (incoming == null) {
            return false;
        }
        Set<String> requested = normalizeIncoming(role, incoming);
        validateSelection(role, requested);
        Set<String> existing = selectedKeys(role);
        if (existing.isEmpty()) {
            existing = keys(RolePermissionPolicy.defaultPermissionsFor(role.getNombre()));
        }
        if (existing.equals(requested)) {
            return false;
        }
        permissions.deleteByRoleId(role.getId());
        permissions.flush();
        List<RolePermission> replacements = new ArrayList<>(records(role, requested));
        replacements.add(record(role, UPGRADE_MODULE, UPGRADE_ACTION));
        permissions.saveAll(replacements);
        users.findByRoleId(role.getId()).forEach(User::incrementSessionVersion);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean allows(Authentication authentication, String module, String action) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return false;
        }
        return users.findByUsernameIgnoreCase(authentication.getName())
                .filter(this::canOperate)
                .map(user -> selectionKeysFor(user.getRole()).contains(RolePermissionPolicy.key(module, action)))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean allowsAny(Authentication authentication, String module, String... actions) {
        for (String action : actions) {
            if (allows(authentication, module, action)) {
                return true;
            }
        }
        return false;
    }

    private boolean canOperate(User user) {
        return Boolean.TRUE.equals(user.getEstado())
                && user.getRole() != null
                && Boolean.TRUE.equals(user.getRole().getEstado())
                && SecurityRoles.isSupported(user.getRole().getNombre());
    }

    private Set<String> selectedKeys(Role role) {
        return permissions.findByRoleIdOrderByModuleKeyAscActionKeyAsc(role.getId()).stream()
                .filter(item -> !UPGRADE_MODULE.equals(item.getModuleKey()))
                .map(item -> RolePermissionPolicy.key(item.getModuleKey(), item.getActionKey()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private List<RolePermissionPolicy.Permission> grouped(String role, Set<String> selected) {
        List<RolePermissionPolicy.Permission> result = new ArrayList<>();
        for (RolePermissionPolicy.Permission available : RolePermissionPolicy.catalogFor(role)) {
            List<String> actions = available.actions().stream()
                    .filter(action -> selected.contains(RolePermissionPolicy.key(available.module(), action)))
                    .toList();
            if (!actions.isEmpty()) {
                result.add(new RolePermissionPolicy.Permission(available.module(), available.label(), actions));
            }
        }
        return List.copyOf(result);
    }

    private Set<String> normalizeIncoming(Role role, List<RolePermissionSelectionPayload> incoming) {
        Set<String> requested = new LinkedHashSet<>();
        for (RolePermissionSelectionPayload selection : incoming) {
            if (selection == null) {
                continue;
            }
            String module = RolePermissionPolicy.normalizeModule(selection.module());
            String action = RolePermissionPolicy.normalizeAction(selection.accion());
            if (!RolePermissionPolicy.isSupportedPermission(role.getNombre(), module, action)) {
                throw new IllegalArgumentException("La selección de permisos contiene una acción no permitida para este perfil.");
            }
            requested.add(RolePermissionPolicy.key(module, action));
        }
        return requested;
    }

    private void validateSelection(Role role, Set<String> requested) {
        Set<String> mandatory = RolePermissionPolicy.mandatorySelectionsFor(role.getNombre());
        if (!requested.containsAll(mandatory)) {
            throw new IllegalArgumentException("Este perfil debe conservar los accesos esenciales para operar de forma segura.");
        }
        for (String selection : requested) {
            String[] parts = selection.split("\\.", 2);
            if (!"ver".equals(parts[1]) && !requested.contains(RolePermissionPolicy.key(parts[0], "ver"))) {
                throw new IllegalArgumentException("Para habilitar una acción, primero debes permitir la consulta de esa área.");
            }
        }
    }

    private Set<String> keys(List<RolePermissionPolicy.Permission> source) {
        Set<String> result = new LinkedHashSet<>();
        source.forEach(permission -> permission.actions().forEach(action -> result.add(RolePermissionPolicy.key(permission.module(), action))));
        return result;
    }

    private List<RolePermission> records(Role role, Set<String> selections) {
        return selections.stream()
                .sorted()
                .map(key -> {
                    String[] parts = key.split("\\.", 2);
                    return record(role, parts[0], parts[1]);
                })
                .toList();
    }

    private RolePermission record(Role role, String module, String action) {
        RolePermission permission = new RolePermission();
        permission.setRole(role);
        permission.setModuleKey(RolePermissionPolicy.normalizeModule(module));
        permission.setActionKey(RolePermissionPolicy.normalizeAction(action));
        return permission;
    }
}
