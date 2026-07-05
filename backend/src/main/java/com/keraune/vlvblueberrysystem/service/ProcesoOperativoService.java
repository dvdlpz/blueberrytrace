package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.FormalizacionResponse;
import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.ProcesoOperativoResponse;
import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.UniformizacionResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.dto.FormalizacionForm;
import com.keraune.vlvblueberrysystem.dto.UniformizacionForm;
import com.keraune.vlvblueberrysystem.entity.Cama;
import com.keraune.vlvblueberrysystem.entity.Formalizacion;
import com.keraune.vlvblueberrysystem.entity.Lote;
import com.keraune.vlvblueberrysystem.entity.LoteTrazable;
import com.keraune.vlvblueberrysystem.entity.Jaba;
import com.keraune.vlvblueberrysystem.entity.Uniformizacion;
import com.keraune.vlvblueberrysystem.repository.FormalizacionRepository;
import com.keraune.vlvblueberrysystem.repository.UniformizacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.ArrayList;

@Service
@Transactional
public class ProcesoOperativoService {
    private static final Set<String> ESTADOS = Set.of("REGISTRADA", "ANULADA");
    private static final Set<String> ORIGENES_UNIFORMIZACION = Set.of("SIEMBRA", "FORMALIZACION", "RECUPERACION");
    private static final Set<String> ORDENES_JABAS = Set.of("MAYOR_A_MENOR", "MENOR_A_MAYOR");

    private final UniformizacionRepository uniformizaciones;
    private final FormalizacionRepository formalizaciones;
    private final AccountService accountService;
    private final OperationReferenceService references;
    private final TraceabilityService traceabilityService;
    private final OperationalQuantityGuard guard;
    private final JabaService jabaService;
    private final RecuperacionRiegoService recuperaciones;
    private final ApiRecordMapper mapper;
    private final AuditService auditService;

    public ProcesoOperativoService(UniformizacionRepository uniformizaciones,
                                   FormalizacionRepository formalizaciones,
                                   AccountService accountService,
                                   OperationReferenceService references,
                                   TraceabilityService traceabilityService,
                                   OperationalQuantityGuard guard,
                                   JabaService jabaService,
                                   RecuperacionRiegoService recuperaciones,
                                   ApiRecordMapper mapper,
                                   AuditService auditService) {
        this.uniformizaciones = uniformizaciones;
        this.formalizaciones = formalizaciones;
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
    public ProcesoOperativoResponse list() {
        List<UniformizacionResponse> uniformizacionRows = uniformizaciones.findAllByOrderByFechaUniformizacionDescIdDesc().stream()
                .map(mapper::uniformizacion).toList();
        List<FormalizacionResponse> formalizacionRows = formalizaciones.findAllByOrderByFechaFormalizacionDescIdDesc().stream()
                .map(mapper::formalizacion).toList();
        return new ProcesoOperativoResponse(mapper.list(uniformizacionRows), mapper.list(formalizacionRows));
    }

    public ProcesoOperativoResponse createUniformizacion(UniformizacionForm form) {
        Uniformizacion entity = new Uniformizacion();
        entity.setUsuarioRegistro(accountService.currentUser());
        applyUniformizacion(entity, form);
        uniformizaciones.save(entity);
        syncRecoveryFromUniformizacion(entity);
        auditService.record("PROCESOS", "CREAR_UNIFORMIZACION", "Uniformizacion", entity.getId(), reference(entity),
                "Se registró una uniformización trazable.");
        return list();
    }

    public ProcesoOperativoResponse updateUniformizacion(Long id, UniformizacionForm form) {
        Uniformizacion entity = uniformizaciones.findById(id).orElseThrow(() -> new IllegalArgumentException("Uniformización no encontrada."));
        ensureTraceable(entity);
        Long traceId = entity.getLoteTrazable().getId();
        if (guard.totalFormalizadas(traceId) > 0 || guard.totalMermas(traceId, "UNIFORMIZACION") > 0) {
            throw new IllegalArgumentException("No se puede editar una uniformización con formalizaciones o mermas posteriores. Registra una corrección justificada.");
        }
        applyUniformizacion(entity, form);
        syncRecoveryFromUniformizacion(entity);
        auditService.record("PROCESOS", "ACTUALIZAR_UNIFORMIZACION", "Uniformizacion", entity.getId(), reference(entity),
                "Se actualizó una uniformización trazable.");
        return list();
    }

    public ProcesoOperativoResponse toggleUniformizacionStatus(Long id) {
        Uniformizacion entity = uniformizaciones.findById(id).orElseThrow(() -> new IllegalArgumentException("Uniformización no encontrada."));
        String next = "REGISTRADA".equalsIgnoreCase(entity.getEstado()) ? "ANULADA" : "REGISTRADA";
        if ("ANULADA".equals(next)) {
            guard.assertCanDeleteUniformizacion(entity);
            assertRecoveryDoesNotBlockReversal(entity);
        } else {
            ensureTraceable(entity);
            guard.validateUniformizacion(entity.getLoteTrazable(), entity.getId(), entity.getRecuperacionRiego() == null ? null : entity.getRecuperacionRiego().getId(), entity.getCantidadInicial(), entity.getCantidadUniformizada(), next);
            guard.validateMovimientoEntreJabas(entity.getLoteTrazable(), entity.getJabaOrigen(), entity.getJabaDestino(), entity.getId(), entity.getCantidadUniformizada(), entity.getCantidadRecuperacion(), next);
        }
        entity.setEstado(next);
        auditService.record("PROCESOS", "CAMBIAR_ESTADO", "Uniformizacion", entity.getId(), reference(entity),
                "La uniformización quedó " + next + ".");
        return list();
    }

    public ProcesoOperativoResponse deleteUniformizacion(Long id) {
        Uniformizacion entity = uniformizaciones.findById(id).orElseThrow(() -> new IllegalArgumentException("Uniformización no encontrada."));
        guard.assertCanDeleteUniformizacion(entity);
        assertRecoveryDoesNotBlockReversal(entity);
        entity.setEstado("ANULADA");
        auditService.record("PROCESOS", "ANULAR", "Uniformizacion", entity.getId(), reference(entity),
                "La uniformización fue anulada; no se eliminó físicamente.");
        return list();
    }

    public ProcesoOperativoResponse createFormalizacion(FormalizacionForm form) {
        Formalizacion entity = new Formalizacion();
        entity.setUsuarioRegistro(accountService.currentUser());
        applyFormalizacion(entity, form);
        formalizaciones.save(entity);
        auditService.record("PROCESOS", "CREAR_FORMALIZACION", "Formalizacion", entity.getId(), reference(entity),
                "Se registró una formalización trazable.");
        return list();
    }

    public ProcesoOperativoResponse updateFormalizacion(Long id, FormalizacionForm form) {
        Formalizacion entity = formalizaciones.findById(id).orElseThrow(() -> new IllegalArgumentException("Formalización no encontrada."));
        ensureTraceable(entity);
        Long traceId = entity.getLoteTrazable().getId();
        long classifications = guard.totalClasificacion(traceId, "PENDIENTE")
                + guard.totalClasificacion(traceId, "VALIDADA")
                + guard.totalClasificacion(traceId, "OBSERVADA");
        if (classifications > 0 || guard.totalMermas(traceId, "FORMALIZACION") > 0) {
            throw new IllegalArgumentException("No se puede editar una formalización con clasificaciones o mermas posteriores. Registra una corrección justificada.");
        }
        applyFormalizacion(entity, form);
        auditService.record("PROCESOS", "ACTUALIZAR_FORMALIZACION", "Formalizacion", entity.getId(), reference(entity),
                "Se actualizó una formalización trazable.");
        return list();
    }

    public ProcesoOperativoResponse toggleFormalizacionStatus(Long id) {
        Formalizacion entity = formalizaciones.findById(id).orElseThrow(() -> new IllegalArgumentException("Formalización no encontrada."));
        String next = "REGISTRADA".equalsIgnoreCase(entity.getEstado()) ? "ANULADA" : "REGISTRADA";
        if ("ANULADA".equals(next)) {
            guard.assertCanDeleteFormalizacion(entity);
        } else {
            ensureTraceable(entity);
            guard.validateFormalizacion(entity.getLoteTrazable(), entity.getId(), entity.getCantidadPlantas(), next);
        }
        entity.setEstado(next);
        auditService.record("PROCESOS", "CAMBIAR_ESTADO", "Formalizacion", entity.getId(), reference(entity),
                "La formalización quedó " + next + ".");
        return list();
    }

    public ProcesoOperativoResponse deleteFormalizacion(Long id) {
        Formalizacion entity = formalizaciones.findById(id).orElseThrow(() -> new IllegalArgumentException("Formalización no encontrada."));
        guard.assertCanDeleteFormalizacion(entity);
        entity.setEstado("ANULADA");
        auditService.record("PROCESOS", "ANULAR", "Formalizacion", entity.getId(), reference(entity),
                "La formalización fue anulada; no se eliminó físicamente.");
        return list();
    }

    private void applyUniformizacion(Uniformizacion entity, UniformizacionForm form) {
        Lote lote = references.lote(form.loteId());
        Cama cama = references.camaDelLote(form.camaId(), form.loteId());
        LoteTrazable trace = traceabilityService.traceForOperation(form.loteTrazableId(), lote.getId(), cama.getId());
        Jaba origin = jabaService.forCama(form.jabaOrigenId(), cama);
        Jaba destination = jabaService.forCama(form.jabaDestinoId(), cama);
        if (origin.getId().equals(destination.getId())) {
            throw new IllegalArgumentException("Selecciona una jaba de destino distinta para mover macetas durante la uniformización.");
        }
        String state = references.allowedState(form.estado(), "REGISTRADA", ESTADOS, "uniformización");
        String originStage = normalize(form.origenOperativo(), ORIGENES_UNIFORMIZACION, "origen de uniformización");
        if (entity.getId() == null && !"REGISTRADA".equals(state)) {
            throw new IllegalArgumentException("Una uniformización nueva debe registrarse inicialmente como registrada.");
        }
        if (form.cantidadUniformizada() + form.cantidadRecuperacion() > form.cantidadInicial()) {
            throw new IllegalArgumentException("La cantidad uniformizada y enviada a recuperación no puede superar la cantidad revisada.");
        }
        validateDate(trace, form.fechaUniformizacion(), "uniformización");
        guard.validateChronology(trace, "UNIFORMIZACION", form.fechaUniformizacion());
        Long linkedRecoveryId = entity.getRecuperacionRiego() == null ? null : entity.getRecuperacionRiego().getId();
        guard.validateUniformizacion(trace, entity.getId(), linkedRecoveryId, form.cantidadInicial(), form.cantidadUniformizada(), state);
        guard.validateMovimientoEntreJabas(trace, origin, destination, entity.getId(), form.cantidadUniformizada(), form.cantidadRecuperacion(), state);
        entity.setLote(lote); entity.setCama(cama); entity.setLoteTrazable(trace); entity.setJabaOrigen(origin); entity.setJabaDestino(destination);
        entity.setFechaUniformizacion(form.fechaUniformizacion()); entity.setCriterio(form.criterio().trim());
        entity.setCantidadInicial(form.cantidadInicial()); entity.setCantidadUniformizada(form.cantidadUniformizada());
        entity.setOrigenOperativo(originStage); entity.setCantidadRecuperacion(form.cantidadRecuperacion()); entity.setMalezasRetiradas(Boolean.TRUE.equals(form.malezasRetiradas()));
        entity.setObservacion(references.trim(form.observacion())); entity.setEstado(state);
    }

    private void applyFormalizacion(Formalizacion entity, FormalizacionForm form) {
        Lote lote = references.lote(form.loteId());
        Cama cama = references.camaDelLote(form.camaId(), form.loteId());
        LoteTrazable trace = traceabilityService.traceForOperation(form.loteTrazableId(), lote.getId(), cama.getId());
        String state = references.allowedState(form.estado(), "REGISTRADA", ESTADOS, "formalización");
        String order = normalize(form.ordenamientoJabas(), ORDENES_JABAS, "ordenamiento de jabas");
        if (entity.getId() == null && !"REGISTRADA".equals(state)) {
            throw new IllegalArgumentException("Una formalización nueva debe registrarse inicialmente como registrada.");
        }
        LinkedHashSet<Long> jabaIds = new LinkedHashSet<>(form.jabaIds());
        if (jabaIds.size() != form.jabaIds().size()) {
            throw new IllegalArgumentException("Selecciona cada jaba una sola vez para la formalización.");
        }
        if (form.cantidadBandejas() != jabaIds.size()) {
            throw new IllegalArgumentException("La cantidad de jabas organizadas debe coincidir con las jabas seleccionadas.");
        }
        List<Jaba> jabasMovidas = new ArrayList<>();
        for (Long jabaId : jabaIds) {
            jabasMovidas.add(jabaService.forCama(jabaId, cama));
        }
        validateDate(trace, form.fechaFormalizacion(), "formalización");
        guard.validateChronology(trace, "FORMALIZACION", form.fechaFormalizacion());
        guard.validateFormalizacion(trace, entity.getId(), form.cantidadPlantas(), state);
        guard.validateFormalizacionEnJabas(trace, jabasMovidas, form.cantidadPlantas(), state);
        entity.setLote(lote); entity.setCama(cama); entity.setLoteTrazable(trace); entity.setFechaFormalizacion(form.fechaFormalizacion());
        entity.setDetalle(form.detalle().trim()); entity.setCantidadBandejas(form.cantidadBandejas()); entity.setJabasMovidas(jabasMovidas); entity.setCantidadPlantas(form.cantidadPlantas());
        entity.setOrdenamientoJabas(order); entity.setObservacion(references.trim(form.observacion())); entity.setEstado(state);
        if ("REGISTRADA".equals(state)) {
            jabaService.applyFormalizationOrder(cama, jabasMovidas);
        }
    }


    private void syncRecoveryFromUniformizacion(Uniformizacion entity) {
        int quantity = entity.getCantidadRecuperacion() == null ? 0 : entity.getCantidadRecuperacion();
        if (quantity <= 0) {
            if (entity.getRecuperacionRiego() != null) {
                recuperaciones.cancelFromUniformizacion(entity.getRecuperacionRiego());
                entity.setRecuperacionRiego(null);
            }
            return;
        }
        String observation = entity.getObservacion() == null ? "Plantas secas u observadas durante uniformización." : entity.getObservacion();
        if (entity.getRecuperacionRiego() == null) {
            entity.setRecuperacionRiego(recuperaciones.createFromUniformizacion(entity.getLoteTrazable(), entity.getJabaOrigen(), entity.getFechaUniformizacion(), quantity, observation));
            return;
        }
        recuperaciones.updateFromUniformizacion(entity.getRecuperacionRiego(), entity.getLoteTrazable(), entity.getJabaOrigen(), entity.getFechaUniformizacion(), quantity, observation);
    }

    private void assertRecoveryDoesNotBlockReversal(Uniformizacion entity) {
        if (entity.getRecuperacionRiego() != null && !"CANCELADA".equalsIgnoreCase(entity.getRecuperacionRiego().getEstado())) {
            throw new IllegalArgumentException("No se puede anular la uniformización porque tiene una recuperación por riego vinculada. Cierra o corrige la recuperación primero.");
        }
    }

    private String normalize(String value, Set<String> allowed, String label) {
        String normalized = value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT).replace('Ó', 'O');
        if ("GRANDE_MEDIANA_PEQUENA".equals(normalized)) normalized = "MAYOR_A_MENOR";
        if ("PEQUENA_MEDIANA_GRANDE".equals(normalized)) normalized = "MENOR_A_MAYOR";
        if (!allowed.contains(normalized)) throw new IllegalArgumentException("El valor de " + label + " no es válido.");
        return normalized;
    }

    private void validateDate(LoteTrazable trace, LocalDate date, String label) {
        if (date == null || date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de " + label + " no puede ser futura.");
        }
        if (date.isBefore(trace.getFechaIngreso())) {
            throw new IllegalArgumentException("La fecha de " + label + " no puede ser anterior al ingreso del lote trazable.");
        }
    }

    private void ensureTraceable(Uniformizacion entity) {
        if (entity.getLoteTrazable() == null) {
            throw new IllegalArgumentException("Esta uniformización histórica se conserva como referencia. Vincúlala desde la normalización del lote trazable antes de modificarla.");
        }
    }

    private void ensureTraceable(Formalizacion entity) {
        if (entity.getLoteTrazable() == null) {
            throw new IllegalArgumentException("Esta formalización histórica se conserva como referencia. Vincúlala desde la normalización del lote trazable antes de modificarla.");
        }
    }

    private String reference(Uniformizacion entity) {
        return entity.getLoteTrazable() == null ? "Uniformización #" + entity.getId() : entity.getLoteTrazable().getCodigo();
    }

    private String reference(Formalizacion entity) {
        return entity.getLoteTrazable() == null ? "Formalización #" + entity.getId() : entity.getLoteTrazable().getCodigo();
    }
}
