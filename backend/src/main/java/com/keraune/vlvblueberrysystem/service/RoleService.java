package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.AdminPayloads.RoleDetailResponse;
import com.keraune.vlvblueberrysystem.api.dto.AdminPayloads.RolePermissionResponse;
import com.keraune.vlvblueberrysystem.api.dto.AdminPayloads.RoleStatePayload;
import com.keraune.vlvblueberrysystem.api.dto.AdminPayloads.RoleUpdatePayload;
import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.UserReferenceResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.entity.Role;
import com.keraune.vlvblueberrysystem.repository.RoleRepository;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import com.keraune.vlvblueberrysystem.security.RolePermissionPolicy;
import com.keraune.vlvblueberrysystem.security.SecurityRoles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class RoleService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ApiRecordMapper mapper;
    private final AuditService auditService;
    private final RolePermissionService permissionService;

    public RoleService(RoleRepository roleRepository, UserRepository userRepository, ApiRecordMapper mapper,
                       AuditService auditService, RolePermissionService permissionService) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.auditService = auditService;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public List<RoleDetailResponse> list() {
        return roleRepository.findAll().stream()
                .sorted((left, right) -> left.getNombre().compareToIgnoreCase(right.getNombre()))
                .map(this::detail)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleDetailResponse find(Long id) {
        return detail(role(id));
    }

    public RoleDetailResponse update(Long id, RoleUpdatePayload payload) {
        Role role = role(id);
        String beforeDescription = role.getDescripcion();
        String beforeColor = role.getColor();
        role.setDescripcion(payload.descripcion().trim());
        if (payload.color() != null && !payload.color().isBlank()) {
            role.setColor(normalizeColor(payload.color()));
        }
        boolean permissionsChanged = permissionService.replace(role, payload.permisos());
        auditService.record(
                "ROLES",
                permissionsChanged ? "ACTUALIZAR_PERMISOS" : "ACTUALIZAR",
                "Role",
                role.getId(),
                role.getNombre(),
                permissionsChanged
                        ? "Se actualizaron la información y los permisos del rol " + visibleName(role.getNombre()) + "."
                        : "Se actualizó la información del rol " + visibleName(role.getNombre()) + ".",
                null,
                "descripcion=" + safe(beforeDescription) + "; color=" + safe(beforeColor),
                "descripcion=" + safe(role.getDescripcion()) + "; color=" + safe(role.getColor())
        );
        return detail(role);
    }

    public RoleDetailResponse changeState(Long id, RoleStatePayload payload) {
        Role role = role(id);
        boolean target = payload.activo();
        if (Boolean.TRUE.equals(role.getEstado()) == target) return detail(role);
        long activeUsers = userRepository.countByRoleNombreIgnoreCaseAndEstadoTrue(role.getNombre());
        if (!target && activeUsers > 0) {
            throw new IllegalArgumentException("No se puede desactivar este rol porque tiene " + activeUsers + " usuarios activos. Reasigna sus perfiles antes de continuar.");
        }
        if (!SecurityRoles.isSupported(role.getNombre())) {
            throw new IllegalArgumentException("El perfil seleccionado no forma parte de los roles corporativos permitidos.");
        }
        role.setEstado(target);
        auditService.record("ROLES", target ? "ACTIVAR" : "DESACTIVAR", "Role", role.getId(), role.getNombre(),
                "Se " + (target ? "activó" : "desactivó") + " el rol " + visibleName(role.getNombre()) + ".");
        return detail(role);
    }

    private Role role(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No se encontró el rol seleccionado."));
    }

    private RoleDetailResponse detail(Role role) {
        List<UserReferenceResponse> users = userRepository.findAllByOrderByNombreCompletoAsc().stream()
                .filter(user -> user.getRole() != null && role.getId().equals(user.getRole().getId()))
                .map(mapper::user)
                .toList();
        Set<String> selected = permissionService.selectionKeysFor(role);
        List<RolePermissionResponse> permissions = RolePermissionPolicy.catalogFor(role.getNombre()).stream()
                .map(permission -> new RolePermissionResponse(
                        permission.module(),
                        permission.label(),
                        permission.actions().stream()
                                .filter(action -> selected.contains(RolePermissionPolicy.key(permission.module(), action)))
                                .toList(),
                        permission.actions()
                ))
                .toList();
        List<String> modules = permissionService.modulesFor(role).stream().sorted().toList();
        List<String> actions = permissions.stream()
                .flatMap(permission -> permission.actions().stream().map(action -> permission.module() + "." + action))
                .toList();
        return new RoleDetailResponse(
                role.getId(),
                role.getNombre(),
                visibleName(role.getNombre()),
                role.getDescripcion(),
                defaultColor(role.getColor()),
                Boolean.TRUE.equals(role.getEstado()),
                users.stream().filter(UserReferenceResponse::activo).count(),
                users.size(),
                permissions,
                modules,
                actions,
                RolePermissionPolicy.mandatorySelectionsFor(role.getNombre()).stream().sorted().toList(),
                users,
                role.getFechaCreacion(),
                role.getFechaActualizacion()
        );
    }

    private String visibleName(String value) {
        if (value == null || value.isBlank()) {
            return "Sin rol";
        }
        String[] words = value.trim().toLowerCase(Locale.ROOT).replace('_', ' ').split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeColor(String color) {
        return color.trim().toUpperCase(Locale.ROOT);
    }

    private String defaultColor(String color) {
        return color == null || color.isBlank() ? "#2563EB" : color;
    }
}
