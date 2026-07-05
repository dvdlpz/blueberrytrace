package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads;
import com.keraune.vlvblueberrysystem.api.dto.TraceabilityPayloads.MermaFormPayload;
import com.keraune.vlvblueberrysystem.api.dto.TraceabilityPayloads.MermaResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.entity.LoteTrazable;
import com.keraune.vlvblueberrysystem.entity.Merma;
import com.keraune.vlvblueberrysystem.repository.MermaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class MermaService {
    private static final Set<String> ETAPAS = Set.of("SIEMBRA", "UNIFORMIZACION", "FORMALIZACION", "CLASIFICACION");
    private final MermaRepository repository;
    private final TraceabilityService traceabilityService;
    private final AccountService accountService;
    private final OperationalQuantityGuard guard;
    private final ApiRecordMapper mapper;
    private final AuditService audit;

    public MermaService(MermaRepository repository, TraceabilityService traceabilityService, AccountService accountService,
                        OperationalQuantityGuard guard, ApiRecordMapper mapper, AuditService audit) {
        this.repository=repository;this.traceabilityService=traceabilityService;this.accountService=accountService;this.guard=guard;this.mapper=mapper;this.audit=audit;
    }
    @Transactional(readOnly = true) public List<MermaResponse> list(){return repository.findAllByOrderByFechaMermaDescIdDesc().stream().map(this::response).toList();}
    public List<MermaResponse> create(MermaFormPayload payload){Merma e=new Merma();e.setUsuarioRegistro(accountService.currentUser());apply(e,payload);repository.save(e);audit.record("MERMAS","CREAR","Merma",e.getId(),e.getLoteTrazable().getCodigo(),"Se registró una merma en "+e.getEtapaOrigen()+".",e.getMotivo());return list();}
    public List<MermaResponse> annul(Long id,String reason){Merma e=repository.findById(id).orElseThrow(()->new IllegalArgumentException("Merma no encontrada"));if(reason==null||reason.isBlank())throw new IllegalArgumentException("Indica el motivo de anulación.");e.setEstado("ANULADA");audit.record("MERMAS","ANULAR","Merma",e.getId(),e.getLoteTrazable().getCodigo(),"Se anuló una merma.",reason);return list();}
    /** Creates a loss linked automatically when recovery irrigation closes with discarded plants. */
    public Merma createRecoveryDiscard(LoteTrazable trace, String originStage, String reason, int quantity, LocalDate date, String observation) {
        if (quantity <= 0) throw new IllegalArgumentException("La cantidad descartada debe ser mayor a cero.");
        String stage = originStage == null ? "CLASIFICACION" : originStage.trim().toUpperCase(Locale.ROOT).replace('Ó', 'O');
        if (!ETAPAS.contains(stage)) stage = "CLASIFICACION";
        Merma entity = new Merma();
        entity.setLoteTrazable(trace);
        entity.setEtapaOrigen(stage);
        entity.setMotivo(reason == null || reason.isBlank() ? "Plantas no recuperadas después de riego" : reason.trim());
        entity.setCantidad(quantity);
        entity.setFechaMerma(date == null ? LocalDate.now() : date);
        entity.setObservacion(observation == null || observation.isBlank() ? null : observation.trim());
        entity.setEstado("REGISTRADA");
        entity.setUsuarioRegistro(accountService.currentUser());
        repository.save(entity);
        audit.record("MERMAS", "CREAR_DESDE_RECUPERACION", "Merma", entity.getId(), trace.getCodigo(), "Se registró una merma por plantas no recuperadas después de riego.", entity.getMotivo());
        return entity;
    }
    private void apply(Merma e,MermaFormPayload p){LoteTrazable t=traceabilityService.activeTrace(p.loteTrazableId());String stage=p.etapaOrigen().trim().toUpperCase(Locale.ROOT);if(!ETAPAS.contains(stage))throw new IllegalArgumentException("Etapa de merma no válida.");if(p.fechaMerma().isAfter(LocalDate.now())||p.fechaMerma().isBefore(t.getFechaIngreso()))throw new IllegalArgumentException("La fecha de merma no es válida para el lote trazable.");guard.validateMermaChronology(t,stage,p.fechaMerma());guard.validateMerma(t,stage,e.getId(),p.cantidad());e.setLoteTrazable(t);e.setEtapaOrigen(stage);e.setMotivo(p.motivo().trim());e.setCantidad(p.cantidad());e.setFechaMerma(p.fechaMerma());e.setObservacion(p.observacion()==null||p.observacion().isBlank()?null:p.observacion().trim());e.setEstado("REGISTRADA");}
    private MermaResponse response(Merma e){return new MermaResponse(e.getId(),new ApiPayloads.ReferenceResponse(e.getLoteTrazable().getId(),e.getLoteTrazable().getCodigo(),e.getLoteTrazable().getVariedad()),e.getEtapaOrigen(),e.getMotivo(),e.getCantidad(),e.getFechaMerma(),e.getObservacion(),e.getEstado(),mapper.user(e.getUsuarioRegistro()),e.getFechaCreacion(),e.getFechaActualizacion());}
}
