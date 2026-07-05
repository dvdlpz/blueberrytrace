package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.JabaResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.dto.JabaForm;
import com.keraune.vlvblueberrysystem.entity.Cama;
import com.keraune.vlvblueberrysystem.entity.Jaba;
import com.keraune.vlvblueberrysystem.repository.JabaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

@Service
@Transactional
public class JabaService {
    private static final Set<String> ESTADOS = Set.of("ACTIVA", "INACTIVA", "MANTENIMIENTO");
    private final JabaRepository repository;
    private final JabaInventoryService inventory;
    private final AccountService accountService;
    private final OperationReferenceService references;
    private final ApiRecordMapper mapper;
    private final AuditService audit;

    public JabaService(JabaRepository repository, JabaInventoryService inventory, AccountService accountService,
                       OperationReferenceService references, ApiRecordMapper mapper, AuditService audit) {
        this.repository = repository;
        this.inventory = inventory;
        this.accountService = accountService;
        this.references = references;
        this.mapper = mapper;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<JabaResponse> list() {
        return repository.findAllByOrderByCodigoAsc().stream()
                .sorted(Comparator.comparing((Jaba item) -> item.getCama() == null ? "" : item.getCama().getCodigo(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(item -> item.getOrdenEnCama() == null ? Integer.MAX_VALUE : item.getOrdenEnCama())
                        .thenComparing(Jaba::getCodigo, String.CASE_INSENSITIVE_ORDER))
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public Jaba forCama(Long jabaId, Cama cama) {
        Jaba jaba = repository.findById(jabaId).orElseThrow(() -> new IllegalArgumentException("Jaba de siembra no encontrada."));
        if (jaba.getCama() == null || !jaba.getCama().getId().equals(cama.getId())) {
            throw new IllegalArgumentException("La jaba seleccionada no pertenece a la cama indicada.");
        }
        if (!"ACTIVA".equalsIgnoreCase(jaba.getEstado())) {
            throw new IllegalArgumentException("La jaba seleccionada no está disponible para registrar una operación.");
        }
        return jaba;
    }

    /**
     * Persists the real physical order defined during formalization. The submitted list
     * is the final sequence for the selected crates; their previous slots are reused so
     * unrelated crates in the same bed keep their position.
     */
    public void applyFormalizationOrder(Cama cama, List<Jaba> orderedJabas) {
        if (cama == null || cama.getId() == null || orderedJabas == null || orderedJabas.isEmpty()) {
            return;
        }
        List<Jaba> selected = new ArrayList<>(orderedJabas);
        Set<Long> ids = new HashSet<>();
        for (Jaba item : selected) {
            if (item == null || item.getId() == null || item.getCama() == null || !Objects.equals(item.getCama().getId(), cama.getId())) {
                throw new IllegalArgumentException("Las jabas formalizadas deben pertenecer a la misma cama.");
            }
            if (!ids.add(item.getId())) {
                throw new IllegalArgumentException("Una jaba no puede repetirse dentro del orden final de formalización.");
            }
        }
        if (selected.size() < 2) {
            return;
        }
        List<Jaba> inBed = repository.findByCamaIdOrderByOrdenEnCamaAscCodigoAsc(cama.getId());
        List<Integer> positions = inBed.stream()
                .filter(item -> ids.contains(item.getId()))
                .map(Jaba::getOrdenEnCama)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        if (positions.size() != selected.size()) {
            throw new IllegalArgumentException("No fue posible resolver las posiciones físicas de las jabas seleccionadas.");
        }
        boolean alreadyOrdered = true;
        for (int index = 0; index < selected.size(); index++) {
            if (!Objects.equals(selected.get(index).getOrdenEnCama(), positions.get(index))) {
                alreadyOrdered = false;
                break;
            }
        }
        if (alreadyOrdered) {
            return;
        }
        // Avoid a temporary collision with the unique (cama_id, orden_en_cama) key.
        for (int index = 0; index < selected.size(); index++) {
            selected.get(index).setOrdenEnCama(-(index + 1));
        }
        repository.flush();
        for (int index = 0; index < selected.size(); index++) {
            selected.get(index).setOrdenEnCama(positions.get(index));
        }
        repository.flush();
    }

    public List<JabaResponse> create(JabaForm form) {
        String code = code(form.codigo());
        if (repository.existsByCodigoIgnoreCase(code)) {
            throw new IllegalArgumentException("Ya existe una jaba de siembra con ese código.");
        }
        Jaba entity = new Jaba();
        entity.setUsuarioRegistro(accountService.currentUser());
        apply(entity, form, true);
        repository.save(entity);
        audit.record("JABAS", "CREAR", "Jaba", entity.getId(), entity.getCodigo(), "Se registró una jaba dentro de la cama productiva.");
        return list();
    }

    public List<JabaResponse> update(Long id, JabaForm form) {
        Jaba entity = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Jaba de siembra no encontrada."));
        String code = code(form.codigo());
        if (!entity.getCodigo().equalsIgnoreCase(code) && repository.existsByCodigoIgnoreCase(code)) {
            throw new IllegalArgumentException("Ya existe una jaba de siembra con ese código.");
        }
        apply(entity, form, false);
        audit.record("JABAS", "ACTUALIZAR", "Jaba", entity.getId(), entity.getCodigo(), "Se actualizó la información de una jaba de siembra.");
        return list();
    }

    public List<JabaResponse> changeState(Long id) {
        Jaba entity = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Jaba de siembra no encontrada."));
        String next = "ACTIVA".equalsIgnoreCase(entity.getEstado()) ? "INACTIVA" : "ACTIVA";
        if (!"ACTIVA".equals(next) && inventory.balance(entity).ocupadas() > 0) {
            throw new IllegalArgumentException("No se puede desactivar la jaba mientras tenga macetas ubicadas. Traslada o regulariza primero las plantas asociadas.");
        }
        entity.setEstado(next);
        audit.record("JABAS", "CAMBIAR_ESTADO", "Jaba", entity.getId(), entity.getCodigo(), "La jaba quedó " + next + ".");
        return list();
    }

    private JabaResponse response(Jaba entity) {
        JabaInventoryService.Balance balance = inventory.balance(entity);
        return new JabaResponse(entity.getId(), entity.getCodigo(), entity.getCapacidadMacetas(), entity.getOrdenEnCama(),
                balance.ocupadas(), balance.disponibles(), balance.enRecuperacion(), entity.getEstado(), entity.getObservacion(),
                mapper.reference(entity.getCama()), mapper.user(entity.getUsuarioRegistro()), entity.getFechaCreacion(), entity.getFechaActualizacion());
    }

    private void apply(Jaba entity, JabaForm form, boolean create) {
        Cama cama = references.cama(form.camaId());
        if (!"ACTIVA".equalsIgnoreCase(cama.getEstado())) {
            throw new IllegalArgumentException("La cama seleccionada no está activa.");
        }
        String state = references.allowedState(form.estado(), "ACTIVA", ESTADOS, "jaba");
        if (create && !"ACTIVA".equals(state)) {
            throw new IllegalArgumentException("Una jaba nueva debe registrarse inicialmente como activa.");
        }
        long occupied = entity.getId() == null ? 0L : inventory.balance(entity).ocupadas();
        if (form.capacidadMacetas() < occupied) {
            throw new IllegalArgumentException("La capacidad de la jaba no puede ser menor que las " + occupied + " macetas que ya contiene.");
        }
        if (entity.getCama() != null && !Objects.equals(entity.getCama().getId(), cama.getId()) && occupied > 0) {
            throw new IllegalArgumentException("No se puede mover una jaba a otra cama mientras tenga macetas ubicadas. Traslada o regulariza primero las plantas.");
        }
        boolean duplicatePosition = repository.findByCamaIdOrderByOrdenEnCamaAscCodigoAsc(cama.getId()).stream()
                .anyMatch(existing -> !Objects.equals(existing.getId(), entity.getId())
                        && Objects.equals(existing.getOrdenEnCama(), form.ordenEnCama()));
        if (duplicatePosition) {
            throw new IllegalArgumentException("Ya existe una jaba en la posición " + form.ordenEnCama() + " de esta cama.");
        }
        entity.setCodigo(code(form.codigo()));
        entity.setCama(cama);
        entity.setCapacidadMacetas(form.capacidadMacetas());
        entity.setOrdenEnCama(form.ordenEnCama());
        entity.setObservacion(references.trim(form.observacion()));
        entity.setEstado(state);
    }

    private String code(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
