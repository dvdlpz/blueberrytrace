package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.SiembraResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.dto.SiembraForm;
import com.keraune.vlvblueberrysystem.entity.Cama;
import com.keraune.vlvblueberrysystem.entity.Lote;
import com.keraune.vlvblueberrysystem.entity.LoteTrazable;
import com.keraune.vlvblueberrysystem.entity.Jaba;
import com.keraune.vlvblueberrysystem.entity.Siembra;
import com.keraune.vlvblueberrysystem.repository.SiembraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class SiembraService {
    private static final Set<String> ESTADOS = Set.of("REGISTRADA", "ANULADA");
    private final SiembraRepository repository;
    private final AccountService accountService;
    private final OperationReferenceService references;
    private final TraceabilityService traceabilityService;
    private final OperationalQuantityGuard quantityGuard;
    private final JabaService jabaService;
    private final ApiRecordMapper mapper;
    private final AuditService auditService;

    public SiembraService(SiembraRepository repository, AccountService accountService, OperationReferenceService references,
                          TraceabilityService traceabilityService, OperationalQuantityGuard quantityGuard, JabaService jabaService, ApiRecordMapper mapper, AuditService auditService) {
        this.repository = repository; this.accountService = accountService; this.references = references;
        this.traceabilityService = traceabilityService; this.quantityGuard = quantityGuard; this.jabaService = jabaService; this.mapper = mapper; this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SiembraResponse> list() { return repository.findAllByOrderByFechaSiembraDescIdDesc().stream().map(mapper::siembra).toList(); }

    public List<SiembraResponse> create(SiembraForm form) {
        Siembra entity = new Siembra(); entity.setUsuarioRegistro(accountService.currentUser()); apply(entity, form); repository.save(entity);
        auditService.record("SIEMBRA", "CREAR", "Siembra", entity.getId(), traceReference(entity), "Se registró una siembra trazable.");
        return list();
    }

    public List<SiembraResponse> update(Long id, SiembraForm form) {
        Siembra entity = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Siembra no encontrada"));
        if (entity.getLoteTrazable() == null) {
            throw new IllegalArgumentException("Esta siembra histórica se conserva como referencia. Vincúlala desde la normalización del lote trazable antes de modificarla.");
        }
        if (quantityGuard.totalUniformizadas(entity.getLoteTrazable().getId()) > 0) {
            throw new IllegalArgumentException("No se puede editar una siembra con procesos posteriores. Registra una corrección o anulación justificada.");
        }
        apply(entity, form); auditService.record("SIEMBRA", "ACTUALIZAR", "Siembra", entity.getId(), traceReference(entity), "Se actualizó una siembra trazable."); return list();
    }

    public List<SiembraResponse> toggleStatus(Long id) {
        Siembra entity = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Siembra no encontrada"));
        String next = "REGISTRADA".equalsIgnoreCase(entity.getEstado()) ? "ANULADA" : "REGISTRADA";
        if ("ANULADA".equals(next)) {
            quantityGuard.assertCanDeleteSiembra(entity);
        } else {
            if (entity.getLoteTrazable() == null) {
                throw new IllegalArgumentException("No se puede reactivar una siembra histórica sin vínculo trazable.");
            }
            quantityGuard.validateSiembra(entity.getCama(), entity.getId(), entity.getCantidadRegistrada(), next);
        }
        entity.setEstado(next);
        auditService.record("SIEMBRA", "CAMBIAR_ESTADO", "Siembra", entity.getId(), traceReference(entity), "La siembra quedó " + next + ".");
        return list();
    }

    public List<SiembraResponse> delete(Long id) {
        Siembra entity = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Siembra no encontrada"));
        quantityGuard.assertCanDeleteSiembra(entity);
        entity.setEstado("ANULADA");
        auditService.record("SIEMBRA", "ANULAR", "Siembra", entity.getId(), traceReference(entity), "La siembra se anuló; no fue eliminada físicamente.");
        return list();
    }

    private void apply(Siembra entity, SiembraForm form) {
        Lote lote = references.lote(form.loteId()); Cama cama = references.camaDelLote(form.camaId(), form.loteId());
        Jaba jaba = jabaService.forCama(form.jabaId(), cama);
        LoteTrazable trace = traceabilityService.traceForOperation(form.loteTrazableId(), lote.getId(), cama.getId());
        String state = references.allowedState(form.estado(), "REGISTRADA", ESTADOS, "siembra");
        if (entity.getId() == null && !"REGISTRADA".equals(state)) {
            throw new IllegalArgumentException("Una siembra nueva debe registrarse inicialmente como registrada. Usa la acción de anulación cuando corresponda.");
        }
        validateDate(trace, form.fechaSiembra(), "siembra");
        quantityGuard.validateSiembra(cama, jaba, entity.getId(), form.cantidadRegistrada(), state);
        entity.setLote(lote); entity.setCama(cama); entity.setJaba(jaba); entity.setLoteTrazable(trace); entity.setFechaSiembra(form.fechaSiembra()); entity.setCantidadRegistrada(form.cantidadRegistrada());
        entity.setObservacion(references.trim(form.observacion())); entity.setEstado(state);
    }

    private void validateDate(LoteTrazable trace, LocalDate date, String stage) {
        if (date == null || date.isAfter(LocalDate.now())) throw new IllegalArgumentException("La fecha de " + stage + " no puede ser futura.");
        if (date.isBefore(trace.getFechaIngreso())) throw new IllegalArgumentException("La fecha de " + stage + " no puede ser anterior al ingreso del lote trazable.");
    }
    private String traceReference(Siembra entity) { return entity.getLoteTrazable() == null ? "Siembra #" + entity.getId() : entity.getLoteTrazable().getCodigo(); }
}
