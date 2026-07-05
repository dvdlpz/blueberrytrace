package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.CamaResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.dto.CamaForm;
import com.keraune.vlvblueberrysystem.entity.Cama;
import com.keraune.vlvblueberrysystem.repository.CamaRepository;
import com.keraune.vlvblueberrysystem.repository.SiembraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class CamaService {
    private static final Set<String> ESTADOS=Set.of("ACTIVA","INACTIVA","MANTENIMIENTO","ARCHIVADA");
    private final CamaRepository camas;private final SiembraRepository siembras;private final AccountService account;private final OperationReferenceService refs;private final OperationalQuantityGuard guard;private final ApiRecordMapper mapper;private final AuditService audit;
    public CamaService(CamaRepository camas,SiembraRepository siembras,AccountService account,OperationReferenceService refs,OperationalQuantityGuard guard,ApiRecordMapper mapper,AuditService audit){this.camas=camas;this.siembras=siembras;this.account=account;this.refs=refs;this.guard=guard;this.mapper=mapper;this.audit=audit;}
    @Transactional(readOnly=true) public List<CamaResponse> list(){return camas.findAllByOrderByCodigoAsc().stream().map(mapper::cama).toList();}
    public List<CamaResponse> create(CamaForm f){String code=f.codigo().trim().toUpperCase(Locale.ROOT);if(camas.existsByCodigoIgnoreCase(code))throw new IllegalArgumentException("Ya existe una cama con ese código.");Cama e=new Cama();e.setUsuarioRegistro(account.currentUser());apply(e,f,false);camas.save(e);audit.record("CAMAS","CREAR","Cama",e.getId(),e.getCodigo(),"Se creó una cama.");return list();}
    public List<CamaResponse> update(Long id,CamaForm f){Cama e=camas.findByIdForUpdate(id).orElseThrow(()->new IllegalArgumentException("Cama no encontrada"));String code=f.codigo().trim().toUpperCase(Locale.ROOT);camas.findAllByOrderByCodigoAsc().stream().filter(o->!o.getId().equals(id)&&code.equalsIgnoreCase(o.getCodigo())).findFirst().ifPresent(o->{throw new IllegalArgumentException("Ya existe otra cama con ese código.");});if(!e.getLote().getId().equals(f.loteId())&&siembras.countByCamaId(id)>0)throw new IllegalArgumentException("No se puede mover una cama con siembras registradas a otro lote.");apply(e,f,true);audit.record("CAMAS","ACTUALIZAR","Cama",e.getId(),e.getCodigo(),"Se actualizó una cama.");return list();}
    public List<CamaResponse> toggleStatus(Long id){Cama e=camas.findByIdForUpdate(id).orElseThrow(()->new IllegalArgumentException("Cama no encontrada"));e.setEstado("ACTIVA".equalsIgnoreCase(e.getEstado())?"INACTIVA":"ACTIVA");audit.record("CAMAS","CAMBIAR_ESTADO","Cama",e.getId(),e.getCodigo(),"La cama quedó "+e.getEstado()+".");return list();}
    private void apply(Cama e,CamaForm f,boolean existing){if(existing)guard.validateCapacity(e,f.capacidadReferencial());e.setLote(refs.lote(f.loteId()));e.setCodigo(f.codigo().trim().toUpperCase(Locale.ROOT));e.setDescripcion(f.descripcion().trim());e.setCapacidadReferencial(f.capacidadReferencial());String state=f.estado().trim().toUpperCase(Locale.ROOT);if(!ESTADOS.contains(state))throw new IllegalArgumentException("Estado de cama no válido.");e.setEstado(state);}
}
