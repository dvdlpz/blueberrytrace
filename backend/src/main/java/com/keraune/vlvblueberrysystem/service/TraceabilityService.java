package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads;
import com.keraune.vlvblueberrysystem.api.dto.TraceabilityPayloads.*;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.entity.*;
import com.keraune.vlvblueberrysystem.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class TraceabilityService {
    private static final Set<String> ESTADOS = Set.of("ACTIVO", "CERRADO", "ANULADO", "ARCHIVADO");
    private static final Set<String> ETAPAS_LEGADO = Set.of("SIEMBRA", "UNIFORMIZACION", "FORMALIZACION", "CLASIFICACION");

    private final LoteTrazableRepository traceRepository;
    private final LoteRepository loteRepository;
    private final CamaRepository camaRepository;
    private final AccountService accountService;
    private final ApiRecordMapper mapper;
    private final OperationalQuantityGuard quantityGuard;
    private final SiembraRepository siembraRepository;
    private final UniformizacionRepository uniformizacionRepository;
    private final FormalizacionRepository formalizacionRepository;
    private final ClasificacionRepository clasificacionRepository;
    private final DespachoRepository despachoRepository;
    private final MermaRepository mermaRepository;
    private final RecuperacionRiegoRepository recuperacionRepository;
    private final EmpaqueRepository empaqueRepository;
    private final RiegoProgramadoRepository riegoRepository;
    private final AuditService auditService;

    public TraceabilityService(LoteTrazableRepository traceRepository, LoteRepository loteRepository, CamaRepository camaRepository,
                               AccountService accountService, ApiRecordMapper mapper, OperationalQuantityGuard quantityGuard,
                               SiembraRepository siembraRepository, UniformizacionRepository uniformizacionRepository,
                               FormalizacionRepository formalizacionRepository, ClasificacionRepository clasificacionRepository,
                               DespachoRepository despachoRepository, MermaRepository mermaRepository, RecuperacionRiegoRepository recuperacionRepository,
                               EmpaqueRepository empaqueRepository, RiegoProgramadoRepository riegoRepository, AuditService auditService) {
        this.traceRepository = traceRepository;
        this.loteRepository = loteRepository;
        this.camaRepository = camaRepository;
        this.accountService = accountService;
        this.mapper = mapper;
        this.quantityGuard = quantityGuard;
        this.siembraRepository = siembraRepository;
        this.uniformizacionRepository = uniformizacionRepository;
        this.formalizacionRepository = formalizacionRepository;
        this.clasificacionRepository = clasificacionRepository;
        this.despachoRepository = despachoRepository;
        this.mermaRepository = mermaRepository;
        this.recuperacionRepository = recuperacionRepository;
        this.empaqueRepository = empaqueRepository;
        this.riegoRepository = riegoRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<LoteTrazableResponse> list() {
        return traceRepository.findAllByOrderByFechaIngresoDescIdDesc().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<LoteTrazableResponse> active() {
        return traceRepository.findByEstadoIgnoreCaseOrderByCodigoAsc("ACTIVO").stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public LoteTrazableResponse find(Long id) {
        return response(trace(id));
    }

    public LoteTrazableResponse create(LoteTrazableFormPayload payload) {
        String code = cleanCode(payload.codigo());
        if (traceRepository.existsByCodigoIgnoreCase(code)) {
            throw new IllegalArgumentException("Ya existe un lote trazable con ese código.");
        }
        LoteTrazable entity = new LoteTrazable();
        entity.setUsuarioResponsable(accountService.currentUser());
        apply(entity, payload, true);
        traceRepository.save(entity);
        entity.setLegadoPendienteNormalizacion(!legacyCandidates(entity).isEmpty());
        auditService.record("TRAZABILIDAD", "CREAR", "LoteTrazable", entity.getId(), entity.getCodigo(),
                "Se creó el lote trazable " + entity.getCodigo() + ".");
        return response(entity);
    }

    public LoteTrazableResponse update(Long id, LoteTrazableFormPayload payload) {
        LoteTrazable entity = traceForUpdate(id);
        if (!entity.getCodigo().equalsIgnoreCase(cleanCode(payload.codigo()))) {
            throw new IllegalArgumentException("El código trazable es inmutable una vez creado.");
        }
        if (hasOperationalMovements(entity.getId()) && (payload.loteFisicoId() == null || payload.camaInicialId() == null)) {
            throw new IllegalArgumentException("Un lote trazable con movimientos debe conservar ubicación física y cama inicial.");
        }
        apply(entity, payload, false);
        entity.setLegadoPendienteNormalizacion(!legacyCandidates(entity).isEmpty());
        auditService.record("TRAZABILIDAD", "ACTUALIZAR", "LoteTrazable", entity.getId(), entity.getCodigo(),
                "Se actualizó el lote trazable " + entity.getCodigo() + ".");
        return response(entity);
    }

    public LoteTrazableResponse changeState(Long id, String requestedState, String reason) {
        LoteTrazable entity = traceForUpdate(id);
        String state = normalizedState(requestedState);
        if (("ANULADO".equals(state) || "ARCHIVADO".equals(state)) && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Indica el motivo para anular o archivar el lote trazable.");
        }
        if ("ANULADO".equals(state) && hasOperationalMovements(entity.getId())) {
            throw new IllegalArgumentException("Un lote trazable con movimientos no se anula; archívalo con motivo o registra una corrección auditada.");
        }
        entity.setEstado(state);
        auditService.record("TRAZABILIDAD", "CAMBIAR_ESTADO", "LoteTrazable", entity.getId(), entity.getCodigo(),
                "El lote trazable quedó " + state + ".", reason);
        return response(entity);
    }

    @Transactional(readOnly = true)
    public LoteTrazableDetailResponse detail(Long id) {
        LoteTrazable trace = trace(id);
        List<Merma> mermas = mermaRepository.findByLoteTrazableId(id);
        List<TimelineEventResponse> events = new ArrayList<>();
        siembraRepository.findByLoteTrazableId(id).forEach(item -> events.add(event("Siembra", item.getEstado(), item.getCantidadRegistrada(), item.getFechaSiembra(), "Siembra #" + item.getId(), item.getObservacion(), userName(item.getUsuarioRegistro()))));
        uniformizacionRepository.findByLoteTrazableId(id).forEach(item -> events.add(event("Uniformización", item.getEstado(), item.getCantidadUniformizada(), item.getFechaUniformizacion(), "Uniformización #" + item.getId(), item.getCriterio(), userName(item.getUsuarioRegistro()))));
        formalizacionRepository.findByLoteTrazableId(id).forEach(item -> events.add(event("Formalización", item.getEstado(), item.getCantidadPlantas(), item.getFechaFormalizacion(), "Formalización #" + item.getId(), item.getDetalle(), userName(item.getUsuarioRegistro()))));
        clasificacionRepository.findByLoteTrazableId(id).forEach(item -> events.add(event("Clasificación", item.getEstado(), item.getCantidad(), item.getFechaClasificacion(), "Clasificación #" + item.getId(), item.getCondicion(), userName(item.getUsuarioRegistro()))));
        despachoRepository.findByLoteTrazableId(id).forEach(item -> events.add(event("Despacho", item.getEstado(), item.getCantidadDespachada(), item.getFechaDespacho(), "Despacho #" + item.getId(), item.getDestino(), userName(item.getUsuarioRegistro()))));
        riegoRepository.findByLoteTrazableId(id).forEach(item -> events.add(event("Riego programado", item.getEstado(), null, item.getFechaEjecucion() == null ? item.getFechaProgramada() : item.getFechaEjecucion(), "Riego #" + item.getId(), item.getEtapaAplicacion(), userName(item.getUsuarioRegistro()))));
        recuperacionRepository.findByLoteTrazableId(id).forEach(item -> events.add(event("Recuperación por riego", item.getEstado(), item.getCantidadIngresada(), item.getFechaIngresoRiego(), "Recuperación #" + item.getId(), item.getEtapaOrigen() + " → " + item.getEtapaRetorno(), userName(item.getUsuarioRegistro()))));
        empaqueRepository.findByLoteTrazableId(id).forEach(item -> events.add(event("Empaque", item.getEstado(), item.getCantidadPlantas(), item.getFechaEmpaque(), "Empaque #" + item.getId(), item.getTipo(), userName(item.getUsuarioRegistro()))));
        mermas.forEach(item -> events.add(event("Merma", item.getEstado(), item.getCantidad(), item.getFechaMerma(), "Merma #" + item.getId(), item.getMotivo(), userName(item.getUsuarioRegistro()))));
        events.sort(Comparator.comparing(TimelineEventResponse::fecha, Comparator.nullsLast(Comparator.reverseOrder())));
        return new LoteTrazableDetailResponse(response(trace), balance(trace), events,
                mermas.stream().map(this::mermaResponse).toList(), legacyCandidates(trace));
    }

    /**
     * Lists only legacy records whose physical lot, bed and date match the selected traceable lot.
     * Dispatches without a source classification remain intentionally excluded because no safe automatic link exists.
     */
    @Transactional(readOnly = true)
    public List<LegacyMovementResponse> legacyCandidates(Long traceId) {
        return legacyCandidates(trace(traceId));
    }

    public LoteTrazableDetailResponse normalizeLegacyMovement(Long traceId, LegacyNormalizationPayload payload) {
        LoteTrazable trace = traceForUpdate(traceId);
        String stage = normalizeStage(payload.etapa());
        String evidence = payload.evidencia().trim();
        switch (stage) {
            case "SIEMBRA" -> {
                Siembra item = siembraRepository.findById(payload.registroId()).orElseThrow(() -> new IllegalArgumentException("Siembra histórica no encontrada."));
                assertLegacyCandidate(trace, item.getLote(), item.getCama(), item.getFechaSiembra(), item.getLoteTrazable());
                quantityGuard.validateSiembra(item.getCama(), item.getId(), item.getCantidadRegistrada(), item.getEstado());
                item.setLoteTrazable(trace);
            }
            case "UNIFORMIZACION" -> {
                Uniformizacion item = uniformizacionRepository.findById(payload.registroId()).orElseThrow(() -> new IllegalArgumentException("Uniformización histórica no encontrada."));
                assertLegacyCandidate(trace, item.getLote(), item.getCama(), item.getFechaUniformizacion(), item.getLoteTrazable());
                quantityGuard.validateChronology(trace, "UNIFORMIZACION", item.getFechaUniformizacion());
                quantityGuard.validateUniformizacion(trace, item.getId(), item.getCantidadInicial(), item.getCantidadUniformizada(), item.getEstado());
                item.setLoteTrazable(trace);
            }
            case "FORMALIZACION" -> {
                Formalizacion item = formalizacionRepository.findById(payload.registroId()).orElseThrow(() -> new IllegalArgumentException("Formalización histórica no encontrada."));
                assertLegacyCandidate(trace, item.getLote(), item.getCama(), item.getFechaFormalizacion(), item.getLoteTrazable());
                quantityGuard.validateChronology(trace, "FORMALIZACION", item.getFechaFormalizacion());
                quantityGuard.validateFormalizacion(trace, item.getId(), item.getCantidadPlantas(), item.getEstado());
                item.setLoteTrazable(trace);
            }
            case "CLASIFICACION" -> {
                Clasificacion item = clasificacionRepository.findById(payload.registroId()).orElseThrow(() -> new IllegalArgumentException("Clasificación histórica no encontrada."));
                assertLegacyCandidate(trace, item.getLote(), item.getCama(), item.getFechaClasificacion(), item.getLoteTrazable());
                quantityGuard.validateChronology(trace, "CLASIFICACION", item.getFechaClasificacion());
                quantityGuard.validateClasificacion(trace, item.getId(), item.getCantidad(), item.getEstado());
                item.setLoteTrazable(trace);
            }
            default -> throw new IllegalArgumentException("Etapa de legado no permitida.");
        }
        trace.setLegadoPendienteNormalizacion(!legacyCandidates(trace).isEmpty());
        auditService.record("TRAZABILIDAD", "NORMALIZAR_LEGADO", "LoteTrazable", trace.getId(), trace.getCodigo(),
                "Se vinculó un movimiento histórico de " + stage.toLowerCase(Locale.ROOT) + " al lote trazable.", evidence);
        return detail(trace.getId());
    }

    public LoteTrazable traceForOperation(Long id, Long loteId, Long camaId) {
        LoteTrazable trace = traceForUpdate(id);
        if (!"ACTIVO".equalsIgnoreCase(trace.getEstado())) {
            throw new IllegalArgumentException("El lote trazable seleccionado no está activo.");
        }
        if (trace.getLoteFisico() == null || !trace.getLoteFisico().getId().equals(loteId)
                || trace.getCamaInicial() == null || !trace.getCamaInicial().getId().equals(camaId)) {
            throw new IllegalArgumentException("El lote trazable no corresponde al invernadero y cama seleccionados.");
        }
        return trace;
    }

    /** Locks an active traceable lot for a dispatch. */
    public LoteTrazable traceForDispatch(Long id, Long loteId) {
        LoteTrazable trace = traceForUpdate(id);
        if (!"ACTIVO".equalsIgnoreCase(trace.getEstado())) {
            throw new IllegalArgumentException("El lote trazable seleccionado no está activo.");
        }
        if (trace.getLoteFisico() == null || !trace.getLoteFisico().getId().equals(loteId)) {
            throw new IllegalArgumentException("El lote trazable no corresponde al invernadero seleccionado.");
        }
        return trace;
    }

    /** Returns an active traceable lot for a loss movement. */
    public LoteTrazable activeTrace(Long id) {
        LoteTrazable trace = traceForUpdate(id);
        if (!"ACTIVO".equalsIgnoreCase(trace.getEstado())) {
            throw new IllegalArgumentException("El lote trazable seleccionado no está activo.");
        }
        return trace;
    }

    @Transactional(readOnly = true)
    public BalanceOperativoResponse balance(LoteTrazable trace) {
        return new BalanceOperativoResponse(
                quantityGuard.totalSembradas(trace.getId()),
                quantityGuard.totalUniformizadas(trace.getId()),
                quantityGuard.totalFormalizadas(trace.getId()),
                quantityGuard.totalClasificacion(trace.getId(), "PENDIENTE"),
                quantityGuard.totalClasificacion(trace.getId(), "VALIDADA"),
                quantityGuard.totalClasificacion(trace.getId(), "OBSERVADA"),
                quantityGuard.totalDespachadas(trace.getId()),
                quantityGuard.totalAnuladas(trace.getId()),
                quantityGuard.totalMermas(trace.getId()),
                quantityGuard.saldoDisponible(trace.getId()),
                quantityGuard.totalEnRecuperacion(trace.getId())
        );
    }

    public LoteTrazableResponse response(LoteTrazable trace) {
        return new LoteTrazableResponse(trace.getId(), trace.getCodigo(), trace.getVariedad(), trace.getProcedencia(), trace.getFechaIngreso(),
                trace.getEstado(), trace.getObservacion(), Boolean.TRUE.equals(trace.getLegadoPendienteNormalizacion()),
                mapper.reference(trace.getLoteFisico()), mapper.reference(trace.getCamaInicial()), mapper.user(trace.getUsuarioResponsable()),
                trace.getFechaCreacion(), trace.getFechaActualizacion());
    }

    private List<LegacyMovementResponse> legacyCandidates(LoteTrazable trace) {
        Long loteId = trace.getLoteFisico().getId();
        Long camaId = trace.getCamaInicial().getId();
        List<LegacyMovementResponse> result = new ArrayList<>();
        siembraRepository.findByLoteIdAndCamaIdAndLoteTrazableIsNull(loteId, camaId).forEach(item -> addIfCandidate(result,
                legacy("SIEMBRA", item.getId(), "Siembra #" + item.getId(), item.getFechaSiembra(), item.getCantidadRegistrada(), item.getEstado(), item.getLote(), item.getCama(), item.getObservacion()), trace));
        uniformizacionRepository.findByLoteIdAndCamaIdAndLoteTrazableIsNull(loteId, camaId).forEach(item -> addIfCandidate(result,
                legacy("UNIFORMIZACION", item.getId(), "Uniformización #" + item.getId(), item.getFechaUniformizacion(), item.getCantidadUniformizada(), item.getEstado(), item.getLote(), item.getCama(), item.getCriterio()), trace));
        formalizacionRepository.findByLoteIdAndCamaIdAndLoteTrazableIsNull(loteId, camaId).forEach(item -> addIfCandidate(result,
                legacy("FORMALIZACION", item.getId(), "Formalización #" + item.getId(), item.getFechaFormalizacion(), item.getCantidadPlantas(), item.getEstado(), item.getLote(), item.getCama(), item.getDetalle()), trace));
        clasificacionRepository.findByLoteIdAndCamaIdAndLoteTrazableIsNull(loteId, camaId).forEach(item -> addIfCandidate(result,
                legacy("CLASIFICACION", item.getId(), "Clasificación #" + item.getId(), item.getFechaClasificacion(), item.getCantidad(), item.getEstado(), item.getLote(), item.getCama(), item.getCondicion()), trace));
        result.sort(Comparator.comparing(LegacyMovementResponse::fecha, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(LegacyMovementResponse::etapa).thenComparing(LegacyMovementResponse::id));
        return result;
    }

    private void addIfCandidate(List<LegacyMovementResponse> values, LegacyMovementResponse candidate, LoteTrazable trace) {
        if (candidate.fecha() != null && !candidate.fecha().isBefore(trace.getFechaIngreso())) values.add(candidate);
    }

    private LegacyMovementResponse legacy(String stage, Long id, String reference, LocalDate date, Integer quantity, String state, Lote lote, Cama cama, String detail) {
        return new LegacyMovementResponse(stage, id, reference, date, quantity, state, mapper.reference(lote), mapper.reference(cama), detail);
    }

    private void assertLegacyCandidate(LoteTrazable trace, Lote lote, Cama cama, LocalDate date, LoteTrazable currentTrace) {
        if (currentTrace != null) throw new IllegalArgumentException("El movimiento histórico ya está vinculado a un lote trazable.");
        if (lote == null || cama == null || !trace.getLoteFisico().getId().equals(lote.getId()) || !trace.getCamaInicial().getId().equals(cama.getId())) {
            throw new IllegalArgumentException("El movimiento no coincide con la ubicación física del lote trazable.");
        }
        if (date == null || date.isBefore(trace.getFechaIngreso())) {
            throw new IllegalArgumentException("La fecha del movimiento no es consistente con el ingreso del lote trazable.");
        }
    }

    private MermaResponse mermaResponse(Merma merma) {
        return new MermaResponse(merma.getId(), new ApiPayloads.ReferenceResponse(merma.getLoteTrazable().getId(), merma.getLoteTrazable().getCodigo(), merma.getLoteTrazable().getVariedad()),
                merma.getEtapaOrigen(), merma.getMotivo(), merma.getCantidad(), merma.getFechaMerma(), merma.getObservacion(), merma.getEstado(),
                mapper.user(merma.getUsuarioRegistro()), merma.getFechaCreacion(), merma.getFechaActualizacion());
    }

    private void apply(LoteTrazable entity, LoteTrazableFormPayload payload, boolean creating) {
        String code = cleanCode(payload.codigo());
        Lote lote = loteRepository.findByIdForUpdate(payload.loteFisicoId()).orElseThrow(() -> new IllegalArgumentException("Invernadero no encontrado"));
        Cama cama = camaRepository.findByIdForUpdate(payload.camaInicialId()).orElseThrow(() -> new IllegalArgumentException("Cama no encontrada"));
        if (!cama.getLote().getId().equals(lote.getId())) {
            throw new IllegalArgumentException("La cama inicial no pertenece al invernadero seleccionado.");
        }
        if (payload.fechaIngreso().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de ingreso no puede ser futura.");
        }
        if (lote.getFechaRegistro() != null && payload.fechaIngreso().isBefore(lote.getFechaRegistro())) {
            throw new IllegalArgumentException("La fecha de ingreso no puede ser anterior al registro del invernadero.");
        }
        entity.setCodigo(code);
        entity.setVariedad(payload.variedad().trim());
        entity.setProcedencia(payload.procedencia().trim());
        entity.setFechaIngreso(payload.fechaIngreso());
        entity.setEstado(normalizedState(payload.estado()));
        entity.setObservacion(trim(payload.observacion()));
        entity.setLoteFisico(lote);
        entity.setCamaInicial(cama);
        if (creating) entity.setLegadoPendienteNormalizacion(false);
    }

    private boolean hasOperationalMovements(Long id) {
        return !siembraRepository.findByLoteTrazableId(id).isEmpty()
                || !uniformizacionRepository.findByLoteTrazableId(id).isEmpty()
                || !formalizacionRepository.findByLoteTrazableId(id).isEmpty()
                || !clasificacionRepository.findByLoteTrazableId(id).isEmpty()
                || !despachoRepository.findByLoteTrazableId(id).isEmpty()
                || !riegoRepository.findByLoteTrazableId(id).isEmpty()
                || !recuperacionRepository.findByLoteTrazableId(id).isEmpty()
                || !empaqueRepository.findByLoteTrazableId(id).isEmpty()
                || !mermaRepository.findByLoteTrazableId(id).isEmpty();
    }

    private LoteTrazable trace(Long id) {
        return traceRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Lote trazable no encontrado."));
    }

    private LoteTrazable traceForUpdate(Long id) {
        return traceRepository.findByIdForUpdate(id).orElseThrow(() -> new IllegalArgumentException("Lote trazable no encontrado."));
    }

    private TimelineEventResponse event(String stage, String state, Integer quantity, LocalDate date, String reference, String detail, String responsible) {
        return new TimelineEventResponse(stage, state, quantity, date, reference, detail, responsible);
    }

    private String userName(User user) { return user == null ? "Sistema" : user.getNombreCompleto(); }
    private String cleanCode(String value) { return value.trim().toUpperCase(Locale.ROOT); }
    private String normalizedState(String value) { String state = value.trim().toUpperCase(Locale.ROOT); if (!ESTADOS.contains(state)) throw new IllegalArgumentException("Estado de lote trazable no válido."); return state; }
    private String normalizeStage(String value) { String stage = value.trim().toUpperCase(Locale.ROOT).replace('Ó', 'O'); if (!ETAPAS_LEGADO.contains(stage)) throw new IllegalArgumentException("Etapa de legado no válida."); return stage; }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
