package com.keraune.vlvblueberrysystem.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class AdminPayloads {
    private AdminPayloads() {
    }

    public record RolePermissionResponse(String module, String label, List<String> actions, List<String> accionesDisponibles) {}

    public record RolePermissionSelectionPayload(
            @NotBlank @Size(max = 80) String module,
            @NotBlank @Size(max = 80) String accion
    ) {}

    public record RoleDetailResponse(
            Long id,
            String codigo,
            String nombreVisible,
            String descripcion,
            String color,
            boolean activo,
            long usuariosActivos,
            long usuariosTotales,
            List<RolePermissionResponse> permisos,
            List<String> modulos,
            List<String> acciones,
            List<String> permisosObligatorios,
            List<ApiPayloads.UserReferenceResponse> usuariosAsignados,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaActualizacion
    ) {}

    public record RoleUpdatePayload(
            @NotBlank @Size(max = 255) String descripcion,
            @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color seleccionado no es válido.") String color,
            List<@Valid RolePermissionSelectionPayload> permisos
    ) {}

    public record RoleStatePayload(@NotNull Boolean activo) {}

    public record PasswordResetPayload(@NotBlank @Size(min = 12, max = 128) String temporaryPassword) {}

    public record AuditResponse(
            Long id,
            ApiPayloads.UserReferenceResponse usuario,
            String rolNombre,
            String modulo,
            String accion,
            String entidadTipo,
            Long entidadId,
            String referencia,
            String descripcion,
            String motivo,
            String valoresAnteriores,
            String valoresPosteriores,
            String ipOrigen,
            String agenteUsuario,
            LocalDateTime fechaEvento
    ) {}
}
