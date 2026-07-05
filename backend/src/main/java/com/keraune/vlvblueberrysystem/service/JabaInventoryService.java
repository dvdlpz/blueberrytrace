package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.entity.Empaque;
import com.keraune.vlvblueberrysystem.entity.Jaba;
import com.keraune.vlvblueberrysystem.entity.RecuperacionRiego;
import com.keraune.vlvblueberrysystem.entity.Uniformizacion;
import com.keraune.vlvblueberrysystem.repository.EmpaqueRepository;
import com.keraune.vlvblueberrysystem.repository.JabaRepository;
import com.keraune.vlvblueberrysystem.repository.ClasificacionRepository;
import com.keraune.vlvblueberrysystem.repository.RecuperacionRiegoRepository;
import com.keraune.vlvblueberrysystem.repository.SiembraRepository;
import com.keraune.vlvblueberrysystem.repository.UniformizacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Computes the physical pot balance of a sowing crate from recorded movements.
 * It does not create a new source of truth: sowings, uniformizations, recovery
 * records and packages remain the auditable operational records.
 */
@Service
@Transactional(readOnly = true)
public class JabaInventoryService {
    private final SiembraRepository siembras;
    private final UniformizacionRepository uniformizaciones;
    private final RecuperacionRiegoRepository recuperaciones;
    private final EmpaqueRepository empaques;
    private final JabaRepository jabas;
    private final ClasificacionRepository clasificaciones;

    public JabaInventoryService(SiembraRepository siembras, UniformizacionRepository uniformizaciones,
                                RecuperacionRiegoRepository recuperaciones, EmpaqueRepository empaques,
                                JabaRepository jabas, ClasificacionRepository clasificaciones) {
        this.siembras = siembras;
        this.uniformizaciones = uniformizaciones;
        this.recuperaciones = recuperaciones;
        this.empaques = empaques;
        this.jabas = jabas;
        this.clasificaciones = clasificaciones;
    }

    public Balance balance(Jaba jaba) {
        return balance(jaba, null, null);
    }

    public Balance balance(Jaba jaba, Long excludedSiembraId, Long excludedUniformizacionId) {
        long sembradas = siembras.findByJabaId(jaba.getId()).stream()
                .filter(item -> item.getLoteTrazable() != null)
                .filter(item -> !same(item.getId(), excludedSiembraId))
                .filter(item -> registered(item.getEstado()))
                .mapToLong(item -> safe(item.getCantidadRegistrada()))
                .sum();

        long entradas = uniformizaciones.findAll().stream()
                .filter(item -> item.getLoteTrazable() != null)
                .filter(item -> !same(item.getId(), excludedUniformizacionId))
                .filter(item -> registered(item.getEstado()))
                .filter(item -> item.getJabaDestino() != null && Objects.equals(item.getJabaDestino().getId(), jaba.getId()))
                .mapToLong(item -> safe(item.getCantidadUniformizada()))
                .sum();

        long salidas = uniformizaciones.findAll().stream()
                .filter(item -> item.getLoteTrazable() != null)
                .filter(item -> !same(item.getId(), excludedUniformizacionId))
                .filter(item -> registered(item.getEstado()))
                .filter(item -> item.getJabaOrigen() != null && Objects.equals(item.getJabaOrigen().getId(), jaba.getId()))
                .mapToLong(item -> safe(item.getCantidadUniformizada()))
                .sum();

        long enRiego = recuperaciones.findAll().stream()
                .filter(item -> item.getJaba() != null && Objects.equals(item.getJaba().getId(), jaba.getId()))
                .mapToLong(this::physicalRecoveryReduction)
                .sum();

        long empacadas = empaques.findAll().stream()
                .filter(item -> !"ANULADO".equalsIgnoreCase(item.getEstado()))
                .filter(item -> belongsToJaba(item, jaba.getId()))
                .mapToLong(item -> safe(item.getCantidadPlantas()))
                .sum();

        long ocupadas = Math.max(0L, sembradas + entradas - salidas - enRiego - empacadas);
        int capacidad = safe(jaba.getCapacidadMacetas());
        long disponibles = Math.max(0L, capacidad - ocupadas);
        return new Balance(capacidad, ocupadas, disponibles, enRiego, empacadas);
    }

    public long occupiedInCama(com.keraune.vlvblueberrysystem.entity.Cama cama, Long excludedSiembraId) {
        if (cama == null || cama.getId() == null) return 0L;
        return jabas.findByCamaIdOrderByOrdenEnCamaAscCodigoAsc(cama.getId()).stream()
                .mapToLong(jaba -> balance(jaba, excludedSiembraId, null).ocupadas())
                .sum();
    }

    public long occupiedForTrace(Jaba jaba, Long traceId) {
        return occupiedForTrace(jaba, traceId, null, null);
    }

    public long occupiedForTrace(Jaba jaba, Long traceId, Long excludedSiembraId, Long excludedUniformizacionId) {
        if (jaba == null || traceId == null) return 0L;
        long sembradas = siembras.findByJabaId(jaba.getId()).stream()
                .filter(item -> item.getLoteTrazable() != null && Objects.equals(item.getLoteTrazable().getId(), traceId))
                .filter(item -> !same(item.getId(), excludedSiembraId))
                .filter(item -> registered(item.getEstado()))
                .mapToLong(item -> safe(item.getCantidadRegistrada()))
                .sum();
        long entradas = uniformizaciones.findByLoteTrazableId(traceId).stream()
                .filter(item -> !same(item.getId(), excludedUniformizacionId))
                .filter(item -> registered(item.getEstado()))
                .filter(item -> item.getJabaDestino() != null && Objects.equals(item.getJabaDestino().getId(), jaba.getId()))
                .mapToLong(item -> safe(item.getCantidadUniformizada()))
                .sum();
        long salidas = uniformizaciones.findByLoteTrazableId(traceId).stream()
                .filter(item -> !same(item.getId(), excludedUniformizacionId))
                .filter(item -> registered(item.getEstado()))
                .filter(item -> item.getJabaOrigen() != null && Objects.equals(item.getJabaOrigen().getId(), jaba.getId()))
                .mapToLong(item -> safe(item.getCantidadUniformizada()))
                .sum();
        long recoveryReduction = recuperaciones.findByLoteTrazableId(traceId).stream()
                .filter(item -> item.getJaba() != null && Objects.equals(item.getJaba().getId(), jaba.getId()))
                .mapToLong(this::physicalRecoveryReduction)
                .sum();
        long packed = empaques.findByLoteTrazableId(traceId).stream()
                .filter(item -> !"ANULADO".equalsIgnoreCase(item.getEstado()))
                .filter(item -> belongsToJaba(item, jaba.getId()))
                .mapToLong(item -> safe(item.getCantidadPlantas()))
                .sum();
        return Math.max(0L, sembradas + entradas - salidas - recoveryReduction - packed);
    }

    public void validateSowing(Jaba jaba, Long currentSiembraId, int quantity, String state) {
        if (!registered(state)) return;
        Balance balance = balance(jaba, currentSiembraId, null);
        if (quantity > balance.disponibles()) {
            throw new IllegalArgumentException("La siembra supera el espacio disponible de la jaba " + jaba.getCodigo() + " (" + balance.disponibles() + " macetas disponibles).");
        }
    }

    public void validateUniformizationMove(Long traceId, Jaba origen, Jaba destino, Long currentUniformizacionId,
                                           int cantidadTrasladada, int cantidadRecuperacion, String state) {
        if (!registered(state)) return;
        if (cantidadTrasladada < 0 || cantidadRecuperacion < 0) {
            throw new IllegalArgumentException("Las cantidades de uniformización no pueden ser negativas.");
        }
        long traceAvailableAtOrigin = occupiedForTrace(origen, traceId, null, currentUniformizacionId);
        long required = (long) cantidadTrasladada + cantidadRecuperacion;
        if (required > traceAvailableAtOrigin) {
            throw new IllegalArgumentException("La jaba de origen " + origen.getCodigo() + " solo contiene " + traceAvailableAtOrigin + " macetas disponibles de este lote trazable.");
        }
        Balance target = balance(destino, null, currentUniformizacionId);
        if (cantidadTrasladada > target.disponibles()) {
            throw new IllegalArgumentException("La jaba de destino " + destino.getCodigo() + " solo tiene espacio para " + target.disponibles() + " macetas.");
        }
    }

    public void validateRecoverySend(Long traceId, Jaba jaba, int quantity) {
        if (jaba == null || quantity <= 0) return;
        long available = occupiedForTrace(jaba, traceId);
        if (quantity > available) {
            throw new IllegalArgumentException("La recuperación supera las macetas ubicadas en la jaba " + jaba.getCodigo() + " para este lote trazable (" + available + ").");
        }
    }

    public void validateFormalizationCrates(Long traceId, java.util.Collection<Jaba> selectedJabas, int quantity, String state) {
        if (!registered(state)) return;
        long available = selectedJabas.stream().mapToLong(jaba -> occupiedForTrace(jaba, traceId)).sum();
        if (available <= 0) {
            throw new IllegalArgumentException("Las jabas seleccionadas no contienen macetas disponibles del lote trazable.");
        }
        if (quantity > available) {
            throw new IllegalArgumentException("La formalización supera las macetas ubicadas en las jabas seleccionadas (" + available + ").");
        }
    }

    public void validateClassificationCrate(Long traceId, Jaba jaba, Long currentClassificationId, int quantity, String state) {
        if (!classificationActive(state)) return;
        long occupied = occupiedForTrace(jaba, traceId);
        long alreadyAllocated = clasificaciones.findByJabaId(jaba.getId()).stream()
                .filter(item -> item.getLoteTrazable() != null && Objects.equals(item.getLoteTrazable().getId(), traceId))
                .filter(item -> !same(item.getId(), currentClassificationId))
                .filter(item -> classificationActive(item.getEstado()))
                .mapToLong(item -> Math.max(0L, safe(item.getCantidad()) - packedFromClassification(item.getId())))
                .sum();
        long available = Math.max(0L, occupied - alreadyAllocated);
        if (quantity > available) {
            throw new IllegalArgumentException("La clasificación supera las macetas disponibles de este lote trazable en la jaba " + jaba.getCodigo() + " (" + available + ").");
        }
    }

    private long physicalRecoveryReduction(RecuperacionRiego recovery) {
        String state = recovery.getEstado() == null ? "" : recovery.getEstado().trim().toUpperCase();
        if ("EN_RIEGO".equals(state)) return safe(recovery.getCantidadIngresada());
        if ("CERRADA".equals(state) || "DESCARTADA".equals(state)) return safe(recovery.getCantidadDescartada());
        return 0L;
    }

    private long packedFromClassification(Long classificationId) {
        return empaques.findByClasificacionId(classificationId).stream()
                .filter(item -> !"ANULADO".equalsIgnoreCase(item.getEstado()))
                .mapToLong(item -> safe(item.getCantidadPlantas()))
                .sum();
    }

    private boolean belongsToJaba(Empaque empaque, Long jabaId) {
        return empaque.getClasificacion() != null
                && empaque.getClasificacion().getJaba() != null
                && Objects.equals(empaque.getClasificacion().getJaba().getId(), jabaId);
    }

    private boolean registered(String state) {
        return state != null && "REGISTRADA".equals(state.trim().toUpperCase());
    }

    private boolean classificationActive(String state) {
        if (state == null) return false;
        String normalized = state.trim().toUpperCase();
        // Observed plants are not allocated for packing. When they are dry, they move to recovery
        // and must receive a new classification after the irrigation result is recorded.
        return "PENDIENTE".equals(normalized) || "VALIDADA".equals(normalized);
    }

    private boolean same(Long value, Long excluded) {
        return excluded != null && Objects.equals(value, excluded);
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }

    public record Balance(long capacidad, long ocupadas, long disponibles, long enRecuperacion, long empacadas) { }
}
