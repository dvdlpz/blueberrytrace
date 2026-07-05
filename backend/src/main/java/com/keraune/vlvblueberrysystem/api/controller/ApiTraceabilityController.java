package com.keraune.vlvblueberrysystem.api.controller;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.ApiResponse;
import com.keraune.vlvblueberrysystem.api.dto.TraceabilityPayloads.*;
import com.keraune.vlvblueberrysystem.service.MermaService;
import com.keraune.vlvblueberrysystem.service.TraceabilityService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ApiTraceabilityController {
    private final TraceabilityService traceabilityService;
    private final MermaService mermaService;

    public ApiTraceabilityController(TraceabilityService traceabilityService, MermaService mermaService) {
        this.traceabilityService = traceabilityService;
        this.mermaService = mermaService;
    }

    @GetMapping("/lotes-trazables")
    @PreAuthorize("@permissionGuard.allows('lotes_trazables', 'ver')")
    public ApiResponse<List<LoteTrazableResponse>> list() { return ApiResponse.ok("Lotes trazables cargados", traceabilityService.list()); }

    @GetMapping("/lotes-trazables/activos")
    @PreAuthorize("@permissionGuard.allows('lotes_trazables', 'ver') or @permissionGuard.allows('trazabilidad', 'ver')")
    public ApiResponse<List<LoteTrazableResponse>> active() { return ApiResponse.ok("Lotes trazables activos cargados", traceabilityService.active()); }

    @GetMapping("/lotes-trazables/{id}")
    @PreAuthorize("@permissionGuard.allows('lotes_trazables', 'ver') or @permissionGuard.allows('trazabilidad', 'ver')")
    public ApiResponse<LoteTrazableDetailResponse> detail(@PathVariable Long id) { return ApiResponse.ok("Trazabilidad cargada", traceabilityService.detail(id)); }

    @GetMapping("/lotes-trazables/{id}/legado")
    @PreAuthorize("@permissionGuard.allows('lotes_trazables', 'normalizar_legado')")
    public ApiResponse<List<LegacyMovementResponse>> legacy(@PathVariable Long id) { return ApiResponse.ok("Movimientos históricos candidatos cargados", traceabilityService.legacyCandidates(id)); }

    @PostMapping("/lotes-trazables/{id}/normalizar-legado")
    @PreAuthorize("hasRole('ADMINISTRADOR') and @permissionGuard.allows('lotes_trazables', 'normalizar_legado')")
    public ApiResponse<LoteTrazableDetailResponse> normalize(@PathVariable Long id, @Valid @RequestBody LegacyNormalizationPayload payload) {
        return ApiResponse.ok("Movimiento histórico vinculado con evidencia", traceabilityService.normalizeLegacyMovement(id, payload));
    }

    @PostMapping("/lotes-trazables")
    @PreAuthorize("@permissionGuard.allows('lotes_trazables', 'crear')")
    public ApiResponse<LoteTrazableResponse> create(@Valid @RequestBody LoteTrazableFormPayload payload) { return ApiResponse.ok("Lote trazable creado", traceabilityService.create(payload)); }

    @PutMapping("/lotes-trazables/{id}")
    @PreAuthorize("@permissionGuard.allows('lotes_trazables', 'editar')")
    public ApiResponse<LoteTrazableResponse> update(@PathVariable Long id, @Valid @RequestBody LoteTrazableFormPayload payload) { return ApiResponse.ok("Lote trazable actualizado", traceabilityService.update(id, payload)); }

    @PatchMapping("/lotes-trazables/{id}/estado")
    @PreAuthorize("@permissionGuard.allows('lotes_trazables', 'cambiar_estado')")
    public ApiResponse<LoteTrazableResponse> state(@PathVariable Long id, @RequestParam String estado, @RequestParam(required = false) String motivo) { return ApiResponse.ok("Estado actualizado", traceabilityService.changeState(id, estado, motivo)); }

    @GetMapping("/mermas")
    @PreAuthorize("@permissionGuard.allows('mermas', 'ver')")
    public ApiResponse<List<MermaResponse>> mermas() { return ApiResponse.ok("Mermas cargadas", mermaService.list()); }

    @PostMapping("/mermas")
    @PreAuthorize("@permissionGuard.allows('mermas', 'crear')")
    public ApiResponse<List<MermaResponse>> createMerma(@Valid @RequestBody MermaFormPayload payload) { return ApiResponse.ok("Merma registrada", mermaService.create(payload)); }

    @PatchMapping("/mermas/{id}/anular")
    @PreAuthorize("@permissionGuard.allows('mermas', 'anular')")
    public ApiResponse<List<MermaResponse>> annulMerma(@PathVariable Long id, @RequestParam String motivo) { return ApiResponse.ok("Merma anulada", mermaService.annul(id, motivo)); }
}
