package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.RecuperacionRiegoResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.dto.RecuperacionRiegoForm;
import com.keraune.vlvblueberrysystem.dto.RecuperacionRiegoStatusForm;
import com.keraune.vlvblueberrysystem.entity.Jaba;
import com.keraune.vlvblueberrysystem.entity.LoteTrazable;
import com.keraune.vlvblueberrysystem.entity.Merma;
import com.keraune.vlvblueberrysystem.entity.RecuperacionRiego;
import com.keraune.vlvblueberrysystem.repository.RecuperacionRiegoRepository;
import com.keraune.vlvblueberrysystem.repository.RiegoProgramadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class RecuperacionRiegoService {
    private static final Set<String> ETAPAS = Set.of("UNIFORMIZACION", "FORMALIZACION", "CLASIFICACION");
    private static final Set<String> RETORNOS = Set.of("UNIFORMIZACION", "CLASIFICACION");
    private final RecuperacionRiegoRepository repository;
    private final RiegoProgramadoRepository riegos;
    private final TraceabilityService traceability;
    private final JabaService jabas;
    private final AccountService account;
    private final MermaService mermas;
    private final OperationalQuantityGuard guard;
    private final ApiRecordMapper mapper;
    private final AuditService audit;

    public RecuperacionRiegoService(RecuperacionRiegoRepository repository, RiegoProgramadoRepository riegos,
                                    TraceabilityService traceability, JabaService jabas, AccountService account,
                                    MermaService mermas, OperationalQuantityGuard guard, ApiRecordMapper mapper,
                                    AuditService audit) {
        this.repository = repository;
        this.riegos = riegos;
        this.traceability = traceability;
        this.jabas = jabas;
        this.account = account;
        this.mermas = mermas;
        this.guard = guard;
        this.mapper = mapper;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<RecuperacionRiegoResponse> list() {
        return repository.findAllByOrderByFechaIngresoRiegoDescIdDesc().stream().map(mapper::recuperacion).toList();
    }

    public List<RecuperacionRiegoResponse> create(RecuperacionRiegoForm form) {
        LoteTrazable trace = traceability.activeTrace(form.loteTrazableId());
        if (form.fechaIngresoRiego() == null || form.fechaIngresoRiego().isAfter(LocalDate.now()) || form.fechaIngresoRiego().isBefore(trace.getFechaIngreso())) {
            throw new IllegalArgumentException("La fecha de ingreso a recuperación debe estar dentro del periodo operativo del lote.");
        }
        String origin = stage(form.etapaOrigen(), ETAPAS, "origen");
        String returnStage = stage(form.etapaRetorno(), RETORNOS, "retorno");
        Jaba jaba = form.jabaId() == null ? null : jabas.forCama(form.jabaId(), trace.getCamaInicial());
        guard.validateRecovery(trace, null, form.cantidadIngresada());
        guard.validateRecuperacionEnJaba(trace, jaba, form.cantidadIngresada());

        RecuperacionRiego entity = newRecovery(trace, jaba, origin, returnStage, form.fechaIngresoRiego(), form.cantidadIngresada(), trim(form.observacion()));
        repository.save(entity);
        audit.record("RECUPERACION", "CREAR", "RecuperacionRiego", entity.getId(), trace.getCodigo(), "Se envió un grupo de plantas a recuperación por riego.");
        return list();
    }

    /**
     * Creates the recovery record produced during uniformization. The caller keeps the
     * reference on the uniformization so the same plants are not recorded twice.
     */
    public RecuperacionRiego createFromUniformizacion(LoteTrazable trace, Jaba origin, LocalDate date, int quantity, String observation) {
        if (quantity <= 0) return null;
        guard.validateRecovery(trace, null, quantity);
        RecuperacionRiego entity = newRecovery(trace, origin, "UNIFORMIZACION", "UNIFORMIZACION", date, quantity, observation);
        repository.save(entity);
        audit.record("RECUPERACION", "CREAR_DESDE_UNIFORMIZACION", "RecuperacionRiego", entity.getId(), trace.getCodigo(),
                "La uniformización envió " + quantity + " plantas a recuperación por riego.");
        return entity;
    }

    /** Creates a recovery record for dry plants detected during classification. */
    public RecuperacionRiego createFromClasificacion(LoteTrazable trace, Jaba jaba, LocalDate date, int quantity, String observation) {
        if (quantity <= 0) return null;
        guard.validateRecovery(trace, null, quantity);
        guard.validateRecuperacionEnJaba(trace, jaba, quantity);
        RecuperacionRiego entity = newRecovery(trace, jaba, "CLASIFICACION", "CLASIFICACION", date, quantity, observation);
        repository.save(entity);
        audit.record("RECUPERACION", "CREAR_DESDE_CLASIFICACION", "RecuperacionRiego", entity.getId(), trace.getCodigo(),
                "La clasificación envió " + quantity + " plantas secas a recuperación por riego.");
        return entity;
    }

    /** Updates a still-open recovery that was created from a classification. */
    public void updateFromClasificacion(RecuperacionRiego entity, LoteTrazable trace, Jaba jaba, LocalDate date, int quantity, String observation) {
        if (!"EN_RIEGO".equalsIgnoreCase(entity.getEstado())) {
            throw new IllegalArgumentException("La recuperación vinculada ya fue cerrada. No se puede cambiar la clasificación que la originó.");
        }
        guard.validateRecovery(trace, entity.getId(), quantity);
        guard.validateRecuperacionEnJaba(trace, jaba, quantity);
        entity.setLoteTrazable(trace);
        entity.setJaba(jaba);
        entity.setFechaIngresoRiego(date);
        entity.setCantidadIngresada(quantity);
        entity.setCantidadRecuperada(0);
        entity.setCantidadDescartada(0);
        entity.setMotivoDescarte(null);
        entity.setObservacion(observation);
        audit.record("RECUPERACION", "ACTUALIZAR_DESDE_CLASIFICACION", "RecuperacionRiego", entity.getId(), trace.getCodigo(),
                "Se actualizó la recuperación vinculada a la clasificación.");
    }

    /** Updates only a still-open automatic recovery linked to a uniformization. */
    public void updateFromUniformizacion(RecuperacionRiego entity, LoteTrazable trace, Jaba origin, LocalDate date, int quantity, String observation) {
        if (!"EN_RIEGO".equalsIgnoreCase(entity.getEstado())) {
            throw new IllegalArgumentException("La recuperación vinculada ya fue cerrada. No se puede cambiar la cantidad de plantas enviadas a riego desde la uniformización.");
        }
        guard.validateRecovery(trace, entity.getId(), quantity);
        entity.setLoteTrazable(trace);
        entity.setJaba(origin);
        entity.setFechaIngresoRiego(date);
        entity.setCantidadIngresada(quantity);
        entity.setCantidadRecuperada(0);
        entity.setCantidadDescartada(0);
        entity.setMotivoDescarte(null);
        entity.setObservacion(observation);
        audit.record("RECUPERACION", "ACTUALIZAR_DESDE_UNIFORMIZACION", "RecuperacionRiego", entity.getId(), trace.getCodigo(),
                "Se actualizó la recuperación vinculada a la uniformización.");
    }

    /** Cancels an open automatic recovery when the originating classification is corrected. */
    public void cancelFromClasificacion(RecuperacionRiego entity) {
        if (!"EN_RIEGO".equalsIgnoreCase(entity.getEstado())) {
            throw new IllegalArgumentException("La recuperación vinculada ya fue cerrada y no puede retirarse de la clasificación.");
        }
        entity.setEstado("CANCELADA");
        audit.record("RECUPERACION", "CANCELAR_DESDE_CLASIFICACION", "RecuperacionRiego", entity.getId(), entity.getLoteTrazable().getCodigo(),
                "La recuperación automática fue cancelada porque la clasificación fue corregida.");
    }

    /** Cancels an open automatic recovery when no plants remain marked for irrigation. */
    public void cancelFromUniformizacion(RecuperacionRiego entity) {
        if (!"EN_RIEGO".equalsIgnoreCase(entity.getEstado())) {
            throw new IllegalArgumentException("La recuperación vinculada ya fue cerrada y no puede retirarse de la uniformización.");
        }
        entity.setEstado("CANCELADA");
        audit.record("RECUPERACION", "CANCELAR_DESDE_UNIFORMIZACION", "RecuperacionRiego", entity.getId(), entity.getLoteTrazable().getCodigo(),
                "La recuperación automática fue cancelada porque la uniformización dejó de enviar plantas a riego.");
    }

    public List<RecuperacionRiegoResponse> close(Long id, RecuperacionRiegoStatusForm form) {
        RecuperacionRiego entity = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Registro de recuperación no encontrado."));
        if (!"EN_RIEGO".equalsIgnoreCase(entity.getEstado())) {
            throw new IllegalArgumentException("Este registro de recuperación ya fue cerrado o cancelado.");
        }
        if (!hasCompletedRecoveryIrrigation(entity)) {
            throw new IllegalArgumentException("Registra un riego de recuperación realizado antes de cerrar este seguimiento.");
        }
        int recovered = form.cantidadRecuperada();
        int discarded = form.cantidadDescartada();
        int incoming = entity.getCantidadIngresada();
        if (recovered + discarded != incoming) {
            throw new IllegalArgumentException("Las plantas recuperadas y descartadas deben sumar exactamente la cantidad ingresada a riego.");
        }
        if (discarded > 0 && (form.motivoDescarte() == null || form.motivoDescarte().isBlank())) {
            throw new IllegalArgumentException("Indica el motivo de descarte para las plantas que no se recuperaron.");
        }
        entity.setCantidadRecuperada(recovered);
        entity.setCantidadDescartada(discarded);
        entity.setMotivoDescarte(trim(form.motivoDescarte()));
        entity.setObservacion(trim(form.observacion()));
        entity.setEstado(discarded == 0 ? "RECUPERADA" : recovered == 0 ? "DESCARTADA" : "CERRADA");
        if (discarded > 0) {
            Merma merma = mermas.createRecoveryDiscard(entity.getLoteTrazable(), entity.getEtapaOrigen(), form.motivoDescarte(), discarded, entity.getFechaIngresoRiego(), entity.getObservacion());
            entity.setMermaGenerada(merma);
        }
        audit.record("RECUPERACION", "CERRAR", "RecuperacionRiego", entity.getId(), entity.getLoteTrazable().getCodigo(),
                "Se cerró la recuperación por riego: " + recovered + " recuperadas y " + discarded + " descartadas.");
        return list();
    }

    private RecuperacionRiego newRecovery(LoteTrazable trace, Jaba jaba, String origin, String returnStage,
                                          LocalDate date, int quantity, String observation) {
        RecuperacionRiego entity = new RecuperacionRiego();
        entity.setLoteTrazable(trace);
        entity.setJaba(jaba);
        entity.setEtapaOrigen(origin);
        entity.setEtapaRetorno(returnStage);
        entity.setFechaIngresoRiego(date);
        entity.setCantidadIngresada(quantity);
        entity.setCantidadRecuperada(0);
        entity.setCantidadDescartada(0);
        entity.setObservacion(observation);
        entity.setEstado("EN_RIEGO");
        entity.setUsuarioRegistro(account.currentUser());
        return entity;
    }

    private boolean hasCompletedRecoveryIrrigation(RecuperacionRiego recovery) {
        return riegos.findByLoteTrazableId(recovery.getLoteTrazable().getId()).stream()
                .filter(item -> "REALIZADO".equalsIgnoreCase(item.getEstado()))
                .filter(item -> "RECUPERACION".equalsIgnoreCase(item.getEtapaAplicacion()))
                .filter(item -> item.getFechaEjecucion() != null && !item.getFechaEjecucion().isBefore(recovery.getFechaIngresoRiego()))
                .anyMatch(item -> recovery.getJaba() == null || (item.getJaba() != null && recovery.getJaba().getId().equals(item.getJaba().getId())));
    }

    private String stage(String value, Set<String> accepted, String label) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('Ó', 'O');
        if (!accepted.contains(normalized)) throw new IllegalArgumentException("La etapa de " + label + " no es válida para recuperación por riego.");
        return normalized;
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
