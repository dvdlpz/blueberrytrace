package com.keraune.vlvblueberrysystem.api.controller;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.ApiResponse;
import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.DashboardApiResponse;
import com.keraune.vlvblueberrysystem.service.DashboardMetricsService;
import com.keraune.vlvblueberrysystem.service.AccountService;
import com.keraune.vlvblueberrysystem.service.RolePermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class ApiDashboardController {
    private final DashboardMetricsService dashboardMetricsService;
    private final AccountService accountService;
    private final RolePermissionService rolePermissionService;

    public ApiDashboardController(DashboardMetricsService dashboardMetricsService, AccountService accountService,
                                  RolePermissionService rolePermissionService) {
        this.dashboardMetricsService = dashboardMetricsService;
        this.accountService = accountService;
        this.rolePermissionService = rolePermissionService;
    }

    @GetMapping("/summary")
    @PreAuthorize("@permissionGuard.allows('dashboard', 'ver')")
    public ApiResponse<DashboardApiResponse> summary() {
        return ApiResponse.ok("Resumen operativo cargado", new DashboardApiResponse(
                dashboardMetricsService.summary(),
                ApiModuleMetadata.modulesFor(rolePermissionService.modulesFor(accountService.currentUser().getRole()))
        ));
    }
}
