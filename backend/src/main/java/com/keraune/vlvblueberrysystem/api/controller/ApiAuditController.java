package com.keraune.vlvblueberrysystem.api.controller;

import com.keraune.vlvblueberrysystem.api.dto.AdminPayloads.AuditResponse;
import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.ApiResponse;
import com.keraune.vlvblueberrysystem.service.AuditQueryService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/auditoria")
public class ApiAuditController {
    private final AuditQueryService service;
    public ApiAuditController(AuditQueryService service){this.service=service;}
    @GetMapping
    @PreAuthorize("@permissionGuard.allows('auditoria', 'ver')")
    public ApiResponse<Page<AuditResponse>> list(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="25")int size,
                                                  @RequestParam(required=false)String modulo,@RequestParam(required=false)String accion,
                                                  @RequestParam(required=false)String usuario,@RequestParam(required=false)String referencia,@RequestParam(required=false)LocalDate desde,
                                                  @RequestParam(required=false)LocalDate hasta){return ApiResponse.ok("Auditoría cargada",service.list(page,size,modulo,accion,usuario,referencia,desde,hasta));}
}
