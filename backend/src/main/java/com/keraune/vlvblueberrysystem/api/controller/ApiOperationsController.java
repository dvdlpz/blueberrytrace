package com.keraune.vlvblueberrysystem.api.controller;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.*;
import com.keraune.vlvblueberrysystem.api.dto.AdminPayloads.PasswordResetPayload;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.dto.*;
import com.keraune.vlvblueberrysystem.repository.CamaRepository;
import com.keraune.vlvblueberrysystem.repository.LoteRepository;
import com.keraune.vlvblueberrysystem.repository.LoteTrazableRepository;
import com.keraune.vlvblueberrysystem.service.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ApiOperationsController {
    private final LoteService loteService;
    private final CamaService camaService;
    private final SiembraService siembraService;
    private final ProcesoOperativoService procesoService;
    private final ClasificacionService clasificacionService;
    private final DespachoService despachoService;
    private final TrazabilidadQueryService trazabilidadService;
    private final AccountService accountService;
    private final LoteRepository loteRepository;
    private final CamaRepository camaRepository;
    private final LoteTrazableRepository loteTrazableRepository;
    private final ApiRecordMapper mapper;
    private final OperationReadinessService operationReadinessService;

    public ApiOperationsController(LoteService loteService, CamaService camaService, SiembraService siembraService,
                                   ProcesoOperativoService procesoService, ClasificacionService clasificacionService,
                                   DespachoService despachoService, TrazabilidadQueryService trazabilidadService,
                                   AccountService accountService, LoteRepository loteRepository, CamaRepository camaRepository,
                                   LoteTrazableRepository loteTrazableRepository, ApiRecordMapper mapper,
                                   OperationReadinessService operationReadinessService) {
        this.loteService = loteService;
        this.camaService = camaService;
        this.siembraService = siembraService;
        this.procesoService = procesoService;
        this.clasificacionService = clasificacionService;
        this.despachoService = despachoService;
        this.trazabilidadService = trazabilidadService;
        this.accountService = accountService;
        this.loteRepository = loteRepository;
        this.camaRepository = camaRepository;
        this.loteTrazableRepository = loteTrazableRepository;
        this.mapper = mapper;
        this.operationReadinessService = operationReadinessService;
    }

    @GetMapping("/operations/readiness")
    @PreAuthorize("@permissionGuard.allows('dashboard', 'ver')")
    public ApiResponse<OperationReadinessResponse> operationReadiness() {
        return ApiResponse.ok("La preparación operativa está disponible.", operationReadinessService.readiness());
    }

    @GetMapping("/catalogs/operations")
    @PreAuthorize("@permissionGuard.allows('dashboard', 'ver')")
    public ApiResponse<CatalogResponse> catalogs() {
        CatalogResponse response = new CatalogResponse(
                loteRepository.findAllByOrderByFechaRegistroDescIdDesc().stream().map(mapper::reference).toList(),
                camaRepository.findAllByOrderByCodigoAsc().stream().map(mapper::reference).toList(),
                accountService.activeRoles(),
                List.of("ACTIVO", "INACTIVO", "MANTENIMIENTO", "ARCHIVADO"),
                List.of("ACTIVA", "INACTIVA", "MANTENIMIENTO", "ARCHIVADA"),
                List.of("REGISTRADA", "ANULADA"),
                List.of("PENDIENTE", "VALIDADA", "OBSERVADA"),
                List.of("REGISTRADO", "DESPACHADO", "OBSERVADO", "CANCELADO"),
                List.of("JABAS", "BINS", "MADERA", "OTRO"),
                List.of("APTO", "OBSERVADO", "REQUIERE_REVISIÓN"),
                loteTrazableRepository.findByEstadoIgnoreCaseOrderByFechaIngresoDesc("ACTIVO").stream().map(mapper::reference).toList()
        );
        return ApiResponse.ok("Catálogos operativos cargados", response);
    }

    @GetMapping("/lotes")
    @PreAuthorize("@permissionGuard.allows('lotes', 'ver')")
    public ApiResponse<ListResponse<LoteResponse>> lotes() { return ApiResponse.ok("Lotes cargados", mapper.list(loteService.list())); }
    @PostMapping("/lotes")
    @PreAuthorize("@permissionGuard.allows('lotes', 'crear')")
    public ApiResponse<ListResponse<LoteResponse>> createLote(@Valid @RequestBody LoteForm form) { return ApiResponse.ok("Lote registrado", mapper.list(loteService.create(form))); }
    @PutMapping("/lotes/{id}")
    @PreAuthorize("@permissionGuard.allows('lotes', 'editar')")
    public ApiResponse<ListResponse<LoteResponse>> updateLote(@PathVariable Long id, @Valid @RequestBody LoteForm form) { return ApiResponse.ok("Lote actualizado", mapper.list(loteService.update(id, form))); }
    @PatchMapping("/lotes/{id}/estado")
    @PreAuthorize("@permissionGuard.allows('lotes', 'cambiar_estado')")
    public ApiResponse<ListResponse<LoteResponse>> toggleLote(@PathVariable Long id) { return ApiResponse.ok("Estado de lote actualizado", mapper.list(loteService.toggleStatus(id))); }
    @DeleteMapping("/lotes/{id}")
    @PreAuthorize("@permissionGuard.allows('lotes', 'archivar')")
    public ApiResponse<ListResponse<LoteResponse>> deleteLote(@PathVariable Long id) { return ApiResponse.ok("Lote archivado", mapper.list(loteService.delete(id))); }

    @GetMapping("/camas")
    @PreAuthorize("@permissionGuard.allows('camas', 'ver')")
    public ApiResponse<ListResponse<CamaResponse>> camas() { return ApiResponse.ok("Camas cargadas", mapper.list(camaService.list())); }
    @PostMapping("/camas")
    @PreAuthorize("@permissionGuard.allows('camas', 'crear')")
    public ApiResponse<ListResponse<CamaResponse>> createCama(@Valid @RequestBody CamaForm form) { return ApiResponse.ok("Cama registrada", mapper.list(camaService.create(form))); }
    @PutMapping("/camas/{id}")
    @PreAuthorize("@permissionGuard.allows('camas', 'editar')")
    public ApiResponse<ListResponse<CamaResponse>> updateCama(@PathVariable Long id, @Valid @RequestBody CamaForm form) { return ApiResponse.ok("Cama actualizada", mapper.list(camaService.update(id, form))); }
    @PatchMapping("/camas/{id}/estado")
    @PreAuthorize("@permissionGuard.allows('camas', 'cambiar_estado')")
    public ApiResponse<ListResponse<CamaResponse>> toggleCama(@PathVariable Long id) { return ApiResponse.ok("Estado de cama actualizado", mapper.list(camaService.toggleStatus(id))); }

    @GetMapping("/siembras")
    @PreAuthorize("@permissionGuard.allows('siembra', 'ver')")
    public ApiResponse<ListResponse<SiembraResponse>> siembras() { return ApiResponse.ok("Siembras cargadas", mapper.list(siembraService.list())); }
    @PostMapping("/siembras")
    @PreAuthorize("@permissionGuard.allows('siembra', 'crear')")
    public ApiResponse<ListResponse<SiembraResponse>> createSiembra(@Valid @RequestBody SiembraForm form) { return ApiResponse.ok("Siembra registrada", mapper.list(siembraService.create(form))); }
    @PutMapping("/siembras/{id}")
    @PreAuthorize("@permissionGuard.allows('siembra', 'editar')")
    public ApiResponse<ListResponse<SiembraResponse>> updateSiembra(@PathVariable Long id, @Valid @RequestBody SiembraForm form) { return ApiResponse.ok("Siembra actualizada", mapper.list(siembraService.update(id, form))); }
    @PatchMapping("/siembras/{id}/estado")
    @PreAuthorize("@permissionGuard.allows('siembra', 'anular')")
    public ApiResponse<ListResponse<SiembraResponse>> toggleSiembra(@PathVariable Long id) { return ApiResponse.ok("Estado de siembra actualizado", mapper.list(siembraService.toggleStatus(id))); }
    @DeleteMapping("/siembras/{id}")
    @PreAuthorize("@permissionGuard.allows('siembra', 'anular')")
    public ApiResponse<ListResponse<SiembraResponse>> deleteSiembra(@PathVariable Long id) { return ApiResponse.ok("Siembra anulada", mapper.list(siembraService.delete(id))); }

    @GetMapping("/procesos")
    @PreAuthorize("@permissionGuard.allows('procesos', 'ver')")
    public ApiResponse<ProcesoOperativoResponse> procesos() { return ApiResponse.ok("Procesos cargados", procesoService.list()); }
    @PostMapping("/procesos/uniformizaciones")
    @PreAuthorize("@permissionGuard.allows('procesos', 'crear')")
    public ApiResponse<ProcesoOperativoResponse> createUniformizacion(@Valid @RequestBody UniformizacionForm form) { return ApiResponse.ok("Uniformización registrada", procesoService.createUniformizacion(form)); }
    @PutMapping("/procesos/uniformizaciones/{id}")
    @PreAuthorize("@permissionGuard.allows('procesos', 'editar')")
    public ApiResponse<ProcesoOperativoResponse> updateUniformizacion(@PathVariable Long id, @Valid @RequestBody UniformizacionForm form) { return ApiResponse.ok("Uniformización actualizada", procesoService.updateUniformizacion(id, form)); }
    @PatchMapping("/procesos/uniformizaciones/{id}/estado")
    @PreAuthorize("@permissionGuard.allows('procesos', 'anular')")
    public ApiResponse<ProcesoOperativoResponse> toggleUniformizacion(@PathVariable Long id) { return ApiResponse.ok("Estado de uniformización actualizado", procesoService.toggleUniformizacionStatus(id)); }
    @DeleteMapping("/procesos/uniformizaciones/{id}")
    @PreAuthorize("@permissionGuard.allows('procesos', 'anular')")
    public ApiResponse<ProcesoOperativoResponse> deleteUniformizacion(@PathVariable Long id) { return ApiResponse.ok("Uniformización anulada", procesoService.deleteUniformizacion(id)); }
    @PostMapping("/procesos/formalizaciones")
    @PreAuthorize("@permissionGuard.allows('procesos', 'crear')")
    public ApiResponse<ProcesoOperativoResponse> createFormalizacion(@Valid @RequestBody FormalizacionForm form) { return ApiResponse.ok("Formalización registrada", procesoService.createFormalizacion(form)); }
    @PutMapping("/procesos/formalizaciones/{id}")
    @PreAuthorize("@permissionGuard.allows('procesos', 'editar')")
    public ApiResponse<ProcesoOperativoResponse> updateFormalizacion(@PathVariable Long id, @Valid @RequestBody FormalizacionForm form) { return ApiResponse.ok("Formalización actualizada", procesoService.updateFormalizacion(id, form)); }
    @PatchMapping("/procesos/formalizaciones/{id}/estado")
    @PreAuthorize("@permissionGuard.allows('procesos', 'anular')")
    public ApiResponse<ProcesoOperativoResponse> toggleFormalizacion(@PathVariable Long id) { return ApiResponse.ok("Estado de formalización actualizado", procesoService.toggleFormalizacionStatus(id)); }
    @DeleteMapping("/procesos/formalizaciones/{id}")
    @PreAuthorize("@permissionGuard.allows('procesos', 'anular')")
    public ApiResponse<ProcesoOperativoResponse> deleteFormalizacion(@PathVariable Long id) { return ApiResponse.ok("Formalización anulada", procesoService.deleteFormalizacion(id)); }

    @GetMapping("/clasificaciones")
    @PreAuthorize("@permissionGuard.allows('clasificacion', 'ver')")
    public ApiResponse<ListResponse<ClasificacionResponse>> clasificaciones() { return ApiResponse.ok("Clasificaciones cargadas", mapper.list(clasificacionService.list())); }
    @PostMapping("/clasificaciones")
    @PreAuthorize("@permissionGuard.allows('clasificacion', 'crear')")
    public ApiResponse<ListResponse<ClasificacionResponse>> createClasificacion(@Valid @RequestBody ClasificacionForm form) { return ApiResponse.ok("Clasificación registrada", mapper.list(clasificacionService.create(form))); }
    @PutMapping("/clasificaciones/{id}")
    @PreAuthorize("@permissionGuard.allows('clasificacion', 'editar')")
    public ApiResponse<ListResponse<ClasificacionResponse>> updateClasificacion(@PathVariable Long id, @Valid @RequestBody ClasificacionForm form) { return ApiResponse.ok("Clasificación actualizada", mapper.list(clasificacionService.update(id, form))); }
    @PatchMapping("/clasificaciones/{id}/estado")
    @PreAuthorize("@permissionGuard.allowsAny('clasificacion', 'validar', 'observar', 'editar')")
    public ApiResponse<ListResponse<ClasificacionResponse>> changeClasificacionStatus(@PathVariable Long id, @RequestParam String estado) { return ApiResponse.ok("Estado de clasificación actualizado", mapper.list(clasificacionService.changeStatus(id, estado))); }

    @GetMapping("/despachos")
    @PreAuthorize("@permissionGuard.allows('despacho', 'ver')")
    public ApiResponse<ListResponse<DespachoResponse>> despachos() { return ApiResponse.ok("Despachos cargados", mapper.list(despachoService.list())); }
    @PostMapping("/despachos")
    @PreAuthorize("@permissionGuard.allows('despacho', 'crear')")
    public ApiResponse<ListResponse<DespachoResponse>> createDespacho(@Valid @RequestBody DespachoForm form) { return ApiResponse.ok("Despacho registrado", mapper.list(despachoService.create(form))); }
    @PutMapping("/despachos/{id}")
    @PreAuthorize("@permissionGuard.allows('despacho', 'editar')")
    public ApiResponse<ListResponse<DespachoResponse>> updateDespacho(@PathVariable Long id, @Valid @RequestBody DespachoForm form) { return ApiResponse.ok("Despacho actualizado", mapper.list(despachoService.update(id, form))); }
    @PatchMapping("/despachos/{id}/estado")
    @PreAuthorize("@permissionGuard.allows('despacho', 'cambiar_estado')")
    public ApiResponse<ListResponse<DespachoResponse>> changeDespachoStatus(@PathVariable Long id, @RequestParam String estado) { return ApiResponse.ok("Estado de despacho actualizado", mapper.list(despachoService.changeStatus(id, estado))); }

    @GetMapping("/reportes/trazabilidad")
    @PreAuthorize("@permissionGuard.allows('trazabilidad', 'ver')")
    public ApiResponse<ListResponse<TrazabilidadResponse>> trazabilidad() { return ApiResponse.ok("Resumen histórico por invernadero cargado", mapper.list(trazabilidadService.list())); }

    @GetMapping("/usuarios")
    public ApiResponse<ListResponse<UserReferenceResponse>> usuarios() { return ApiResponse.ok("Usuarios cargados", mapper.list(accountService.listUsers())); }
    @PostMapping("/usuarios")
    public ApiResponse<ListResponse<UserReferenceResponse>> createUsuario(@Valid @RequestBody UserFormPayload payload) { return ApiResponse.ok("Usuario creado", mapper.list(accountService.createUser(payload))); }
    @PutMapping("/usuarios/{id}")
    public ApiResponse<ListResponse<UserReferenceResponse>> updateUsuario(@PathVariable Long id, @Valid @RequestBody UserFormPayload payload) { return ApiResponse.ok("Usuario actualizado", mapper.list(accountService.updateUser(id, payload))); }
    @PatchMapping("/usuarios/{id}/estado")
    public ApiResponse<ListResponse<UserReferenceResponse>> toggleUsuario(@PathVariable Long id) { return ApiResponse.ok("Estado de usuario actualizado", mapper.list(accountService.toggleUserStatus(id))); }

    @PatchMapping("/usuarios/{id}/password")
    public ApiResponse<Void> resetUsuarioPassword(@PathVariable Long id, @Valid @RequestBody PasswordResetPayload payload) {
        accountService.resetPassword(id, payload);
        return ApiResponse.ok("Contraseña temporal configurada", null);
    }
}
