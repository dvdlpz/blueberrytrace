package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.entity.*;
import com.keraune.vlvblueberrysystem.repository.*;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

/**
 * Validates the traceable operational chain. Legacy records without a traceable
 * lot remain readable, but new movements are evaluated by their traceable lot.
 */
@Service
public class OperationalQuantityGuard {
    private final SiembraRepository siembraRepository;
    private final UniformizacionRepository uniformizacionRepository;
    private final FormalizacionRepository formalizacionRepository;
    private final ClasificacionRepository clasificacionRepository;
    private final DespachoRepository despachoRepository;
    private final MermaRepository mermaRepository;
    private final RecuperacionRiegoRepository recuperacionRepository;
    private final JabaInventoryService jabaInventory;

    public OperationalQuantityGuard(SiembraRepository siembraRepository, UniformizacionRepository uniformizacionRepository,
                                    FormalizacionRepository formalizacionRepository, ClasificacionRepository clasificacionRepository,
                                    DespachoRepository despachoRepository, MermaRepository mermaRepository,
                                    RecuperacionRiegoRepository recuperacionRepository, JabaInventoryService jabaInventory) {
        this.siembraRepository = siembraRepository;
        this.uniformizacionRepository = uniformizacionRepository;
        this.formalizacionRepository = formalizacionRepository;
        this.clasificacionRepository = clasificacionRepository;
        this.despachoRepository = despachoRepository;
        this.mermaRepository = mermaRepository;
        this.recuperacionRepository = recuperacionRepository;
        this.jabaInventory = jabaInventory;
    }

    public void validateCapacity(Cama cama, int proposedCapacity) {
        long sown = totalActiveSiembras(cama.getId(), null);
        if (proposedCapacity < sown) {
            throw new IllegalArgumentException("La capacidad de la cama no puede ser menor que las plantas sembradas activas (" + sown + ").");
        }
    }

    public void validateSiembra(Cama cama, Long currentId, int quantity, String state) {
        if (!isRegistered(state)) return;
        long total = totalActiveSiembras(cama.getId(), currentId) + quantity;
        int capacity = cama.getCapacidadReferencial() == null ? 0 : cama.getCapacidadReferencial();
        if (total > capacity) {
            throw new IllegalArgumentException("La cantidad sembrada excede la capacidad referencial de la cama (" + capacity + ").");
        }
    }

    /** Uses current crate occupancy for new operations, allowing a bed to be reused after its plants leave it. */
    public void validateSiembra(Cama cama, Jaba jaba, Long currentId, int quantity, String state) {
        if (!isRegistered(state)) return;
        int capacity = cama.getCapacidadReferencial() == null ? 0 : cama.getCapacidadReferencial();
        long occupied = jabaInventory.occupiedInCama(cama, currentId);
        if (occupied + quantity > capacity) {
            throw new IllegalArgumentException("La siembra supera la capacidad disponible de la cama (" + Math.max(0L, capacity - occupied) + " macetas disponibles).");
        }
        jabaInventory.validateSowing(jaba, currentId, quantity, state);
    }


    /** Verifies the current physical pot capacity of a propagation crate. */
    public void validateSiembraEnJaba(Jaba jaba, Long currentId, int quantity, String state) {
        jabaInventory.validateSowing(jaba, currentId, quantity, state);
    }

    /** Validates the physical movement of pots between two sowing crates. */
    public void validateMovimientoEntreJabas(LoteTrazable trace, Jaba origen, Jaba destino, Long currentUniformizacionId,
                                             int cantidadTrasladada, int cantidadRecuperacion, String state) {
        jabaInventory.validateUniformizationMove(trace.getId(), origen, destino, currentUniformizacionId,
                cantidadTrasladada, cantidadRecuperacion, state);
    }

    public void validateRecuperacionEnJaba(LoteTrazable trace, Jaba jaba, int quantity) {
        jabaInventory.validateRecoverySend(trace.getId(), jaba, quantity);
    }

    public void validateFormalizacionEnJabas(LoteTrazable trace, java.util.Collection<Jaba> jabas, int quantity, String state) {
        jabaInventory.validateFormalizationCrates(trace.getId(), jabas, quantity, state);
    }

    public void validateClasificacionEnJaba(LoteTrazable trace, Jaba jaba, Long currentId, int quantity, String state) {
        jabaInventory.validateClassificationCrate(trace.getId(), jaba, currentId, quantity, state);
    }

    public void validateUniformizacion(LoteTrazable trace, Long currentId, int initial, int quantity, String state) {
        validateUniformizacion(trace, currentId, null, initial, quantity, state);
    }

    public void validateUniformizacion(LoteTrazable trace, Long currentId, Long linkedRecoveryId, int initial, int quantity, String state) {
        if (quantity > initial) throw new IllegalArgumentException("La cantidad uniformizada no puede ser mayor que la cantidad revisada.");
        if (!isRegistered(state)) return;
        long available = availableInGrowingArea(trace.getId(), linkedRecoveryId);
        if (initial > available || quantity > available) {
            throw new IllegalArgumentException("La uniformización supera las plantas disponibles del lote trazable (" + available + ").");
        }
    }

    public void validateFormalizacion(LoteTrazable trace, Long currentId, int quantity, String state) {
        if (!isRegistered(state)) return;
        if (totalUniformizadas(trace.getId()) <= 0) {
            throw new IllegalArgumentException("Registra al menos una uniformización antes de formalizar jabas.");
        }
        long available = availableInGrowingArea(trace.getId(), null);
        if (quantity > available) {
            throw new IllegalArgumentException("La formalización supera las plantas disponibles del lote trazable (" + available + ").");
        }
    }

    public void validateClasificacion(LoteTrazable trace, Long currentId, int quantity, String state) {
        if (!isClassificationActive(state)) return;
        if (totalFormalizadas(trace.getId()) <= 0) {
            throw new IllegalArgumentException("Registra una formalización antes de clasificar plantas.");
        }
        long available = availableInGrowingArea(trace.getId(), null);
        long accumulated = totalClassifications(trace.getId(), currentId) + quantity;
        if (accumulated > available) {
            throw new IllegalArgumentException("La clasificación acumulada excede las plantas disponibles del lote trazable (" + available + ").");
        }
    }

    /** Enforces chronological order between recorded stages of the same traceable lot. */
    public void validateChronology(LoteTrazable trace, String targetStage, java.time.LocalDate date) {
        if (date == null) throw new IllegalArgumentException("La fecha operativa es obligatoria.");
        java.time.LocalDate predecessor = switch (targetStage) {
            case "UNIFORMIZACION" -> latestRegisteredSiembra(trace.getId());
            case "FORMALIZACION" -> latestRegisteredUniformizacion(trace.getId());
            case "CLASIFICACION" -> latestRegisteredFormalizacion(trace.getId());
            default -> null;
        };
        if (predecessor != null && date.isBefore(predecessor)) {
            throw new IllegalArgumentException("La fecha de " + targetStage.toLowerCase(Locale.ROOT) + " no puede ser anterior a la última etapa previa registrada (" + predecessor + ").");
        }
    }

    public void validateDespacho(LoteTrazable trace, Clasificacion classification, Long currentId, int quantity, String state) {
        if (!isDispatchCounted(state)) return;
        if (!is("VALIDADA", classification.getEstado())) {
            throw new IllegalArgumentException("Solo se puede despachar una clasificación validada.");
        }
        if (classification.getLoteTrazable() == null || !Objects.equals(classification.getLoteTrazable().getId(), trace.getId())) {
            throw new IllegalArgumentException("La clasificación seleccionada no pertenece al lote trazable indicado.");
        }
        long usedFromClassification = despachoRepository.findByClasificacionId(classification.getId()).stream()
                .filter(item -> !sameId(item.getId(), currentId))
                .filter(item -> isDispatchCounted(item.getEstado()))
                .mapToLong(item -> safe(item.getCantidadDespachada())).sum();
        long availableFromClassification = Math.max(0L, safe(classification.getCantidad()) - usedFromClassification);
        if (quantity > availableFromClassification) {
            throw new IllegalArgumentException("El despacho excede el saldo disponible de la clasificación validada (" + availableFromClassification + ").");
        }
        long availableAcrossTrace = Math.max(0L, totalClasificacion(trace.getId(), "VALIDADA") - totalDespachadas(trace.getId(), currentId) - totalMermas(trace.getId(), "CLASIFICACION"));
        if (quantity > availableAcrossTrace) {
            throw new IllegalArgumentException("El despacho excede el saldo operativo disponible del lote trazable (" + availableAcrossTrace + ").");
        }
    }

    /** Ensures that dry plants sent to recovery do not exceed the live growing stock. */
    public void validateRecovery(LoteTrazable trace, Long currentRecoveryId, int quantity) {
        long available = availableInGrowingArea(trace.getId(), currentRecoveryId);
        if (quantity <= 0 || quantity > available) {
            throw new IllegalArgumentException("La cantidad enviada a recuperación supera las plantas disponibles del lote trazable (" + available + ").");
        }
    }

    public void validateMermaChronology(LoteTrazable trace, String stage, java.time.LocalDate date) {
        java.time.LocalDate latest = switch (stage) {
            case "SIEMBRA" -> latestRegisteredSiembra(trace.getId());
            case "UNIFORMIZACION" -> latestRegisteredUniformizacion(trace.getId());
            case "FORMALIZACION" -> latestRegisteredFormalizacion(trace.getId());
            case "CLASIFICACION" -> latestValidatedClasificacion(trace.getId());
            default -> throw new IllegalArgumentException("Etapa de merma no válida.");
        };
        if (latest == null || date == null || date.isBefore(latest)) {
            throw new IllegalArgumentException("La fecha de merma debe ser igual o posterior a la etapa de origen registrada.");
        }
    }

    public void validateMerma(LoteTrazable trace, String stage, Long currentId, int quantity) {
        long available = switch (stage) {
            case "SIEMBRA", "UNIFORMIZACION", "FORMALIZACION" -> Math.max(0L, totalSembradas(trace.getId()) - totalMermasAntesDeClasificacion(trace.getId(), currentId));
            case "CLASIFICACION" -> Math.max(0L, totalClasificacion(trace.getId(), "VALIDADA") - totalDespachadas(trace.getId()) - totalMermas(trace.getId(), stage, currentId));
            default -> throw new IllegalArgumentException("Etapa de merma no válida.");
        };
        if (quantity > available) {
            throw new IllegalArgumentException("La merma excede el saldo disponible de la etapa " + stage + " (" + available + ").");
        }
    }

    public void assertCanDeleteSiembra(Siembra siembra) {
        if (siembra.getLoteTrazable() != null && (totalUniformizadas(siembra.getLoteTrazable().getId()) > 0 || totalMermas(siembra.getLoteTrazable().getId(), "SIEMBRA") > 0)) {
            throw new IllegalArgumentException("No se puede eliminar la siembra porque el lote trazable ya tiene movimientos posteriores.");
        }
    }

    public void assertCanDeleteUniformizacion(Uniformizacion item) {
        if (item.getLoteTrazable() != null && (totalFormalizadas(item.getLoteTrazable().getId()) > 0 || totalMermas(item.getLoteTrazable().getId(), "UNIFORMIZACION") > 0)) {
            throw new IllegalArgumentException("No se puede eliminar la uniformización porque el lote trazable ya tiene movimientos posteriores.");
        }
    }

    public void assertCanDeleteFormalizacion(Formalizacion item) {
        if (item.getLoteTrazable() != null && (totalClassifications(item.getLoteTrazable().getId(), null) > 0 || totalMermas(item.getLoteTrazable().getId(), "FORMALIZACION") > 0)) {
            throw new IllegalArgumentException("No se puede eliminar la formalización porque el lote trazable ya tiene movimientos posteriores.");
        }
    }

    public long totalSembradas(Long traceId) { return totalSembradas(traceId, null); }
    public long totalUniformizadas(Long traceId) { return totalUniformizadas(traceId, null); }
    public long totalFormalizadas(Long traceId) { return totalFormalizadas(traceId, null); }
    public long totalDespachadas(Long traceId) {
        return totalDespachadas(traceId, null);
    }
    public long totalClasificacion(Long traceId, String state) {
        return clasificacionRepository.findByLoteTrazableId(traceId).stream().filter(item -> is(state, item.getEstado())).mapToLong(item -> safe(item.getCantidad())).sum();
    }
    public long totalAnuladas(Long traceId) {
        return siembraRepository.findByLoteTrazableId(traceId).stream().filter(item -> is("ANULADA", item.getEstado())).count()
                + uniformizacionRepository.findByLoteTrazableId(traceId).stream().filter(item -> is("ANULADA", item.getEstado())).count()
                + formalizacionRepository.findByLoteTrazableId(traceId).stream().filter(item -> is("ANULADA", item.getEstado())).count()
                + clasificacionRepository.findByLoteTrazableId(traceId).stream().filter(item -> is("ANULADA", item.getEstado())).count()
                + despachoRepository.findByLoteTrazableId(traceId).stream().filter(item -> is("CANCELADO", item.getEstado())).count();
    }
    public long totalMermas(Long traceId) { return mermaRepository.findByLoteTrazableId(traceId).stream().filter(item -> isRegistered(item.getEstado())).mapToLong(item -> safe(item.getCantidad())).sum(); }
    public long totalMermas(Long traceId, String stage) { return totalMermas(traceId, stage, null); }
    public long saldoDisponible(Long traceId) { return Math.max(0L, totalClasificacion(traceId, "VALIDADA") - totalDespachadas(traceId) - totalMermas(traceId, "CLASIFICACION")); }

    public long totalEnRecuperacion(Long traceId) {
        return recuperacionRepository.findByLoteTrazableId(traceId).stream()
                .filter(item -> is("EN_RIEGO", item.getEstado()))
                .mapToLong(item -> safe(item.getCantidadIngresada()))
                .sum();
    }

    private long availableInGrowingArea(Long traceId, Long excludedRecoveryId) {
        long recovery = recuperacionRepository.findByLoteTrazableId(traceId).stream()
                .filter(item -> !sameId(item.getId(), excludedRecoveryId))
                .filter(item -> is("EN_RIEGO", item.getEstado()))
                .mapToLong(item -> safe(item.getCantidadIngresada()))
                .sum();
        return Math.max(0L, totalSembradas(traceId) - totalMermasAntesDeClasificacion(traceId) - recovery);
    }

    private long totalMermasAntesDeClasificacion(Long traceId) {
        return totalMermasAntesDeClasificacion(traceId, null);
    }

    private long totalMermasAntesDeClasificacion(Long traceId, Long excludedId) {
        return mermaRepository.findByLoteTrazableId(traceId).stream()
                .filter(item -> !sameId(item.getId(), excludedId))
                .filter(item -> isRegistered(item.getEstado()))
                .filter(item -> is("SIEMBRA", item.getEtapaOrigen()) || is("UNIFORMIZACION", item.getEtapaOrigen()) || is("FORMALIZACION", item.getEtapaOrigen()))
                .mapToLong(item -> safe(item.getCantidad())).sum();
    }

    private java.time.LocalDate latestRegisteredSiembra(Long traceId) {
        return siembraRepository.findByLoteTrazableId(traceId).stream().filter(item -> isRegistered(item.getEstado())).map(Siembra::getFechaSiembra).filter(Objects::nonNull).max(java.time.LocalDate::compareTo).orElse(null);
    }
    private java.time.LocalDate latestRegisteredUniformizacion(Long traceId) {
        return uniformizacionRepository.findByLoteTrazableId(traceId).stream().filter(item -> isRegistered(item.getEstado())).map(Uniformizacion::getFechaUniformizacion).filter(Objects::nonNull).max(java.time.LocalDate::compareTo).orElse(null);
    }
    private java.time.LocalDate latestRegisteredFormalizacion(Long traceId) {
        return formalizacionRepository.findByLoteTrazableId(traceId).stream().filter(item -> isRegistered(item.getEstado())).map(Formalizacion::getFechaFormalizacion).filter(Objects::nonNull).max(java.time.LocalDate::compareTo).orElse(null);
    }

    private java.time.LocalDate latestValidatedClasificacion(Long traceId) {
        return clasificacionRepository.findByLoteTrazableId(traceId).stream().filter(item -> is("VALIDADA", item.getEstado())).map(Clasificacion::getFechaClasificacion).filter(Objects::nonNull).max(java.time.LocalDate::compareTo).orElse(null);
    }

    private long totalSembradas(Long traceId, Long excludedId) { return siembraRepository.findByLoteTrazableId(traceId).stream().filter(item -> !sameId(item.getId(), excludedId)).filter(item -> isRegistered(item.getEstado())).mapToLong(item -> safe(item.getCantidadRegistrada())).sum(); }
    private long totalUniformizadas(Long traceId, Long excludedId) { return uniformizacionRepository.findByLoteTrazableId(traceId).stream().filter(item -> !sameId(item.getId(), excludedId)).filter(item -> isRegistered(item.getEstado())).mapToLong(item -> safe(item.getCantidadUniformizada())).sum(); }
    private long totalFormalizadas(Long traceId, Long excludedId) { return formalizacionRepository.findByLoteTrazableId(traceId).stream().filter(item -> !sameId(item.getId(), excludedId)).filter(item -> isRegistered(item.getEstado())).mapToLong(item -> safe(item.getCantidadPlantas())).sum(); }
    private long totalClassifications(Long traceId, Long excludedId) { return clasificacionRepository.findByLoteTrazableId(traceId).stream().filter(item -> !sameId(item.getId(), excludedId)).filter(item -> isClassificationActive(item.getEstado())).mapToLong(item -> safe(item.getCantidad())).sum(); }
    private long totalDespachadas(Long traceId, Long excludedId) { return despachoRepository.findByLoteTrazableId(traceId).stream().filter(item -> !sameId(item.getId(), excludedId)).filter(item -> isDispatchCounted(item.getEstado())).mapToLong(item -> safe(item.getCantidadDespachada())).sum(); }
    private long totalMermas(Long traceId, String stage, Long excludedId) { return mermaRepository.findByLoteTrazableId(traceId).stream().filter(item -> !sameId(item.getId(), excludedId)).filter(item -> isRegistered(item.getEstado())).filter(item -> is(stage, item.getEtapaOrigen())).mapToLong(item -> safe(item.getCantidad())).sum(); }
    private long totalActiveSiembras(Long bedId, Long excludedId) {
        return siembraRepository.findByCamaId(bedId).stream()
                .filter(item -> item.getLoteTrazable() != null)
                .filter(item -> !sameId(item.getId(), excludedId))
                .filter(item -> isRegistered(item.getEstado()))
                .mapToLong(item -> safe(item.getCantidadRegistrada()))
                .sum();
    }
    private boolean sameId(Long value, Long expected) { return expected != null && Objects.equals(value, expected); }
    private boolean isRegistered(String state) { return is("REGISTRADA", state); }
    private boolean isClassificationActive(String state) { return is("PENDIENTE", state) || is("VALIDADA", state); }
    private boolean isDispatchCounted(String state) { return is("DESPACHADO", state); }
    private boolean is(String expected, String actual) { return actual != null && expected.equals(actual.trim().toUpperCase(Locale.ROOT)); }
    private long safe(Integer value) { return value == null ? 0L : value.longValue(); }
}
