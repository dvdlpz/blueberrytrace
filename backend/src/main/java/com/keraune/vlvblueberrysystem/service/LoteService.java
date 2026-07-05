package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.LoteResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.dto.LoteForm;
import com.keraune.vlvblueberrysystem.entity.Lote;
import com.keraune.vlvblueberrysystem.repository.CamaRepository;
import com.keraune.vlvblueberrysystem.repository.LoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class LoteService {
    private static final Set<String> ESTADOS=Set.of("ACTIVO","INACTIVO","MANTENIMIENTO","ARCHIVADO");
    private final LoteRepository lotes; private final CamaRepository camas; private final AccountService account; private final ApiRecordMapper mapper; private final AuditService audit;
    public LoteService(LoteRepository lotes,CamaRepository camas,AccountService account,ApiRecordMapper mapper,AuditService audit){this.lotes=lotes;this.camas=camas;this.account=account;this.mapper=mapper;this.audit=audit;}
    @Transactional(readOnly=true) public List<LoteResponse> list(){return lotes.findAllByOrderByFechaRegistroDescIdDesc().stream().map(mapper::lote).toList();}
    public List<LoteResponse> create(LoteForm f){String code=f.codigo().trim().toUpperCase(Locale.ROOT);if(lotes.existsByCodigoIgnoreCase(code))throw new IllegalArgumentException("Ya existe un lote con ese código.");Lote e=new Lote();e.setUsuarioRegistro(account.currentUser());apply(e,f);lotes.save(e);audit.record("LOTES","CREAR","Lote",e.getId(),e.getCodigo(),"Se creó un lote físico.");return list();}
    public List<LoteResponse> update(Long id,LoteForm f){Lote e=lotes.findByIdForUpdate(id).orElseThrow(()->new IllegalArgumentException("Lote no encontrado"));String code=f.codigo().trim().toUpperCase(Locale.ROOT);lotes.findAllByOrderByFechaRegistroDescIdDesc().stream().filter(o->!o.getId().equals(id)&&code.equalsIgnoreCase(o.getCodigo())).findFirst().ifPresent(o->{throw new IllegalArgumentException("Ya existe otro lote con ese código.");});apply(e,f);audit.record("LOTES","ACTUALIZAR","Lote",e.getId(),e.getCodigo(),"Se actualizó un lote físico.");return list();}
    public List<LoteResponse> toggleStatus(Long id){Lote e=lotes.findByIdForUpdate(id).orElseThrow(()->new IllegalArgumentException("Lote no encontrado"));e.setEstado("ACTIVO".equalsIgnoreCase(e.getEstado())?"INACTIVO":"ACTIVO");audit.record("LOTES","CAMBIAR_ESTADO","Lote",e.getId(),e.getCodigo(),"El lote quedó "+e.getEstado()+".");return list();}
    public List<LoteResponse> delete(Long id){Lote e=lotes.findByIdForUpdate(id).orElseThrow(()->new IllegalArgumentException("Lote no encontrado"));e.setEstado("ARCHIVADO");audit.record("LOTES","ARCHIVAR","Lote",e.getId(),e.getCodigo(),"El lote fue archivado; no se eliminó físicamente.");return list();}
    private void apply(Lote e,LoteForm f){e.setCodigo(f.codigo().trim().toUpperCase(Locale.ROOT));e.setDescripcion(f.descripcion().trim());e.setCultivo(trim(f.cultivo()));e.setVariedad(trim(f.variedad()));e.setFechaRegistro(f.fechaRegistro());e.setObservacion(trim(f.observacion()));String state=f.estado().trim().toUpperCase(Locale.ROOT);if(!ESTADOS.contains(state))throw new IllegalArgumentException("Estado de lote no válido.");e.setEstado(state);}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
}
