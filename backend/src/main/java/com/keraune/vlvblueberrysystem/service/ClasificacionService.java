package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.ClasificacionResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.dto.ClasificacionForm;
import com.keraune.vlvblueberrysystem.entity.Cama;
import com.keraune.vlvblueberrysystem.entity.Clasificacion;
import com.keraune.vlvblueberrysystem.entity.Lote;
import com.keraune.vlvblueberrysystem.entity.LoteTrazable;
import com.keraune.vlvblueberrysystem.entity.Jaba;
import com.keraune.vlvblueberrysystem.repository.ClasificacionRepository;
import com.keraune.vlvblueberrysystem.repository.DespachoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ClasificacionService {
    private static final Set<String> ESTADOS = Set.of("PENDIENTE", "VALIDADA", "OBSERVADA", "ANULADA");

    private final ClasificacionRepository repository;
    private final DespachoRepository despachoRepository;
    private final AccountService accountService;
    private final OperationReferenceService references;
    private final TraceabilityService traceabilityService;
    private final OperationalQuantityGuard guard;
    private final JabaService jabaService;
    private final RecuperacionRiegoService recuperaciones;
    private final ApiRecordMapper mapper;
    private final AuditService auditService;

    public ClasificacionService(ClasificacionRepository repository,
                                DespachoRepository despachoRepository,
                                AccountService accountService,
                                OperationReferenceService references,
                                TraceabilityService traceabilityService,
                                OperationalQuantityGuard guard,
                                JabaService jabaService,
                                RecuperacionRiegoService recuperaciones,
                                ApiRecordMapper mapper,
                                AuditService auditService) {
        this.repository = repository;
        this.despachoRepository = despachoRepository;
        this.accountService = accountService;
        this.references = references;
        this.traceabilityService = traceabilityService;
        this.guard = guard;
        this.jabaService = jabaService;
        this.recuperaciones = recuperaciones;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ClasificacionResponse> list() {
        return repository.findAllByOrderByFechaClasificacionDescIdDesc().stream().map(mapper::clasificacion).toList();
    }

    public List<ClasificacionResponse> create(ClasificacionForm form) {
        Clasificacion entity = new Clasificacion();
        entity.setUsuarioRegistro(accountService.currentUser());
        apply(entity, form);
        repository.save(entity);
        syncRecoveryFromClassification(entity);
        auditService.record("CLASIFICACION", "CREAR", "Clasificacion", entity.getId(), reference(entity),
                "Se registró una clasificación vinculada al lote trazable.");
        return list();
    }

    public List<ClasificacionResponse> update(Long id, ClasificacionForm form) {
        Clasificacion entity = get(id);
        ensureTraceable(entity);
        if (!despachoRepository.findByClasificacionId(id).isEmpty()) {
            throw new IllegalArgumentException("No se puede editar una clasificación que ya tiene despachos vinculados.");
        }
        apply(entity, form);
        syncRecoveryFromClassification(entity);
        auditService.record("CLASIFICACION", "ACTUALIZAR", "Clasificacion", entity.getId(), reference(entity),
                "Se actualizó una clasificación trazable.");
        return list();
    }

    public List<ClasificacionResponse> changeStatus(Long id, String requestedState) {
        Clasificacion entity = get(id);
        String state = references.allowedState(requestedState, "PENDIENTE", ESTADOS, "clasificación");

        if (entity.getLoteTrazable() == null) {
            if ("ANULADA".equals(state)) {
                entity.setEstado(state);
                auditService.record("CLASIFICACION", "ANULAR_HISTORICO", "Clasificacion", entity.getId(), reference(entity),
                        "Se anuló una clasificación histórica sin vínculo trazable.");
                return list();
            }
            throw new IllegalArgumentException("Esta clasificación histórica se conserva como referencia y no puede incorporarse al saldo actual sin un lote trazable verificable.");
        }

        if (("ANULADA".equals(state) || "OBSERVADA".equals(state)) && !despachoRepository.findByClasificacionId(id).isEmpty()) {
            throw new IllegalArgumentException("No se puede cambiar el estado de una clasificación que tiene despachos vinculados.");
        }
        if ("VALIDADA".equals(state) && entity.getRecuperacionRiego() != null) {
            throw new IllegalArgumentException("Esta clasificación corresponde a plantas enviadas a recuperación por riego. Registra una nueva clasificación cuando las plantas recuperadas vuelvan a estar aptas.");
        }
        if ("ANULADA".equals(state) && entity.getRecuperacionRiego() != null && "EN_RIEGO".equalsIgnoreCase(entity.getRecuperacionRiego().getEstado())) {
            throw new IllegalArgumentException("No se puede anular esta clasificación mientras la recuperación por riego siga abierta. Cierra o corrige la recuperación primero.");
        }
        guard.validateClasificacion(entity.getLoteTrazable(), entity.getId(), entity.getCantidad(), state);
        entity.setEstado(state);
        auditService.record("CLASIFICACION", "CAMBIAR_ESTADO", "Clasificacion", entity.getId(), reference(entity),
                "La clasificación quedó " + state + ".");
        return list();
    }

    private void apply(Clasificacion entity, ClasificacionForm form) {
        Lote lote = references.lote(form.loteId());
        Cama cama = references.camaDelLote(form.camaId(), form.loteId());
        Jaba jaba = jabaService.forCama(form.jabaId(), cama);
        LoteTrazable trace = traceabilityService.traceForOperation(form.loteTrazableId(), lote.getId(), cama.getId());
        String requestedState = references.allowedState(form.estado(), "PENDIENTE", ESTADOS, "clasificación");
        boolean requiresRecovery = requiresRecovery(form.estadoPlanta(), form.condicion());
        String state = requiresRecovery ? "OBSERVADA" : requestedState;
        if (entity.getId() == null && !"PENDIENTE".equals(requestedState)) {
            throw new IllegalArgumentException("Una clasificación nueva debe iniciar pendiente de validación. La validación se realiza mediante la acción correspondiente.");
        }

        validateDate(trace, form.fechaClasificacion());
        guard.validateChronology(trace, "CLASIFICACION", form.fechaClasificacion());
        guard.validateClasificacion(trace, entity.getId(), form.cantidad(), state);
        guard.validateClasificacionEnJaba(trace, jaba, entity.getId(), form.cantidad(), state);

        entity.setLote(lote);
        entity.setCama(cama);
        entity.setJaba(jaba);
        entity.setLoteTrazable(trace);
        entity.setFechaClasificacion(form.fechaClasificacion());
        entity.setEstadoPlanta(form.estadoPlanta().trim());
        entity.setTamano(form.tamano().trim());
        entity.setCondicion(form.condicion().trim());
        entity.setCantidad(form.cantidad());
        entity.setObservacion(references.trim(form.observacion()));
        entity.setEstado(state);
    }

    private void syncRecoveryFromClassification(Clasificacion entity) {
        if (!requiresRecovery(entity.getEstadoPlanta(), entity.getCondicion())) {
            if (entity.getRecuperacionRiego() != null) {
                recuperaciones.cancelFromClasificacion(entity.getRecuperacionRiego());
                entity.setRecuperacionRiego(null);
            }
            return;
        }
        String observation = entity.getObservacion() == null || entity.getObservacion().isBlank()
                ? "Plantas secas u observadas durante la clasificación."
                : entity.getObservacion();
        if (entity.getRecuperacionRiego() == null) {
            entity.setRecuperacionRiego(recuperaciones.createFromClasificacion(entity.getLoteTrazable(), entity.getJaba(), entity.getFechaClasificacion(), entity.getCantidad(), observation));
            return;
        }
        recuperaciones.updateFromClasificacion(entity.getRecuperacionRiego(), entity.getLoteTrazable(), entity.getJaba(), entity.getFechaClasificacion(), entity.getCantidad(), observation);
    }

    private boolean requiresRecovery(String estadoPlanta, String condicion) {
        return "RECUPERACION".equalsIgnoreCase(estadoPlanta)
                || "SECA".equalsIgnoreCase(condicion);
    }

    private void validateDate(LoteTrazable trace, LocalDate date) {
        if (date == null || date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de clasificación no puede ser futura.");
        }
        if (date.isBefore(trace.getFechaIngreso())) {
            throw new IllegalArgumentException("La fecha de clasificación no puede ser anterior al ingreso del lote trazable.");
        }
    }

    private Clasificacion get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Clasificación no encontrada."));
    }

    private void ensureTraceable(Clasificacion entity) {
        if (entity.getLoteTrazable() == null) {
            throw new IllegalArgumentException("Esta clasificación histórica se conserva como referencia. Vincúlala desde la normalización del lote trazable antes de modificarla.");
        }
    }

    private String reference(Clasificacion entity) {
        return entity.getLoteTrazable() == null ? "Clasificación #" + entity.getId() : entity.getLoteTrazable().getCodigo();
    }
}
