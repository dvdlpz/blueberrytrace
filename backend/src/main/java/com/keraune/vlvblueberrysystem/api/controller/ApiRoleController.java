package com.keraune.vlvblueberrysystem.api.controller;

import com.keraune.vlvblueberrysystem.api.dto.AdminPayloads.RoleDetailResponse;
import com.keraune.vlvblueberrysystem.api.dto.AdminPayloads.RoleStatePayload;
import com.keraune.vlvblueberrysystem.api.dto.AdminPayloads.RoleUpdatePayload;
import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.ApiResponse;
import com.keraune.vlvblueberrysystem.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ApiRoleController {
    private final RoleService roleService;

    public ApiRoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<List<RoleDetailResponse>> list() {
        return ApiResponse.ok("Roles cargados", roleService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleDetailResponse> find(@PathVariable Long id) {
        return ApiResponse.ok("Rol cargado", roleService.find(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleDetailResponse> update(@PathVariable Long id, @Valid @RequestBody RoleUpdatePayload payload) {
        return ApiResponse.ok("Perfil y permisos actualizados", roleService.update(id, payload));
    }

    @PatchMapping("/{id}/estado")
    public ApiResponse<RoleDetailResponse> state(@PathVariable Long id, @Valid @RequestBody RoleStatePayload payload) {
        return ApiResponse.ok("Estado del rol actualizado", roleService.changeState(id, payload));
    }
}
