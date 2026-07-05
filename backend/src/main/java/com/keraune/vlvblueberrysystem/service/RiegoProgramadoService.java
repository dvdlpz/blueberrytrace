package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.RiegoProgramadoResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.dto.RiegoProgramadoForm;
import com.keraune.vlvblueberrysystem.dto.RiegoRealizadoForm;
import com.keraune.vlvblueberrysystem.entity.Cama;
import com.keraune.vlvblueberrysystem.entity.Jaba;
import com.keraune.vlvblueberrysystem.entity.LoteTrazable;
import com.keraune.vlvblueberrysystem.entity.RiegoProgramado;
import com.keraune.vlvblueberrysystem.repository.RiegoProgramadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class RiegoProgramadoService {
    private static final Set<String> ETAPAS = Set.of("CRECIMIENTO", "RECUPERACION");
    private final RiegoProgramadoRepository repository;
    private final TraceabilityService traceability;
    private final OperationReferenceService references;
    private final JabaService jabas;
    private final AccountService account;
    private final ApiRecordMapper mapper;
    private final AuditService audit;

    public RiegoProgramadoService(RiegoProgramadoRepository repository, TraceabilityService traceability,
                                  OperationReferenceService references, JabaService jabas, AccountService account,
                                  ApiRecordMapper mapper, AuditService audit) {
        this.repository = repository;
        this.traceability = traceability;
        this.references = references;
        this.jabas = jabas;
        this.account = account;
        this.mapper = mapper;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<RiegoProgramadoResponse> list() {
        return repository.findAllByOrderByFechaProgramadaDescHoraProgramadaDescIdDesc().stream().map(mapper::riego).toList();
    }

    public List<RiegoProgramadoResponse> create(RiegoProgramadoForm form) {
        Cama cama = references.cama(form.camaId());
        LoteTrazable trace = traceability.traceForOperation(form.loteTrazableId(), cama.getLote().getId(), cama.getId());
        if (form.fechaProgramada().isBefore(trace.getFechaIngreso())) {
            throw new IllegalArgumentException("La fecha programada no puede ser anterior al ingreso del lote trazable.");
        }
        Jaba jaba = form.jabaId() == null ? null : jabas.forCama(form.jabaId(), cama);
        RiegoProgramado entity = new RiegoProgramado();
        entity.setLoteTrazable(trace);
        entity.setCama(cama);
        entity.setJaba(jaba);
        entity.setFechaProgramada(form.fechaProgramada());
        entity.setHoraProgramada(form.horaProgramada());
        entity.setEtapaAplicacion(stage(form.etapaAplicacion()));
        entity.setObservacion(trim(form.observacion()));
        entity.setEstado("PROGRAMADO");
        entity.setUsuarioRegistro(account.currentUser());
        repository.save(entity);
        audit.record("RIEGOS", "PROGRAMAR", "RiegoProgramado", entity.getId(), trace.getCodigo(), "Se programó un riego para la operación del lote trazable.");
        return list();
    }

    public List<RiegoProgramadoResponse> complete(Long id, RiegoRealizadoForm form) {
        RiegoProgramado entity = get(id);
        if (!"PROGRAMADO".equalsIgnoreCase(entity.getEstado())) {
            throw new IllegalArgumentException("Este riego ya fue realizado o cancelado.");
        }
        if (form.fechaEjecucion().isAfter(LocalDate.now()) || form.fechaEjecucion().isBefore(entity.getLoteTrazable().getFechaIngreso())) {
            throw new IllegalArgumentException("La fecha de ejecución del riego no es válida para este lote trazable.");
        }
        if (form.fechaEjecucion().equals(entity.getFechaProgramada()) && form.horaEjecucion().isBefore(entity.getHoraProgramada())) {
            throw new IllegalArgumentException("La hora de ejecución no puede ser anterior a la hora programada del mismo día.");
        }
        entity.setFechaEjecucion(form.fechaEjecucion());
        entity.setHoraEjecucion(form.horaEjecucion());
        entity.setObservacion(trim(form.observacion()));
        entity.setEstado("REALIZADO");
        audit.record("RIEGOS", "REALIZAR", "RiegoProgramado", entity.getId(), entity.getLoteTrazable().getCodigo(), "Se registró la ejecución del riego programado.");
        return list();
    }

    public List<RiegoProgramadoResponse> cancel(Long id) {
        RiegoProgramado entity = get(id);
        if (!"PROGRAMADO".equalsIgnoreCase(entity.getEstado())) {
            throw new IllegalArgumentException("Solo se puede cancelar un riego que aún está programado.");
        }
        entity.setEstado("CANCELADO");
        audit.record("RIEGOS", "CANCELAR", "RiegoProgramado", entity.getId(), entity.getLoteTrazable().getCodigo(), "Se canceló un riego programado sin eliminar el historial.");
        return list();
    }

    private RiegoProgramado get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Riego programado no encontrado."));
    }

    private String stage(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('Ó', 'O');
        if (!ETAPAS.contains(normalized)) {
            throw new IllegalArgumentException("Selecciona una etapa de aplicación válida para el riego.");
        }
        return normalized;
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
