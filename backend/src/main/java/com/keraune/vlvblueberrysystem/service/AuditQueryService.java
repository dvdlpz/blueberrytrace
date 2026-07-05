package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.api.dto.AdminPayloads.AuditResponse;
import com.keraune.vlvblueberrysystem.api.mapper.ApiRecordMapper;
import com.keraune.vlvblueberrysystem.entity.AuditoriaOperacion;
import com.keraune.vlvblueberrysystem.repository.AuditoriaOperacionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class AuditQueryService {
    private final AuditoriaOperacionRepository repository;
    private final ApiRecordMapper mapper;
    public AuditQueryService(AuditoriaOperacionRepository repository, ApiRecordMapper mapper){this.repository=repository;this.mapper=mapper;}
    public Page<AuditResponse> list(int page,int size,String modulo,String accion,String usuario,String referencia,LocalDate desde,LocalDate hasta){
        Specification<AuditoriaOperacion> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if(has(modulo))spec=spec.and((root,q,cb)->cb.equal(root.get("modulo"),modulo.trim().toUpperCase()));
        if(has(accion))spec=spec.and((root,q,cb)->cb.equal(root.get("accion"),accion.trim().toUpperCase()));
        if(has(usuario))spec=spec.and((root,q,cb)->cb.equal(cb.lower(root.join("usuario").get("username")),usuario.trim().toLowerCase()));
        if(has(referencia))spec=spec.and((root,q,cb)->cb.like(cb.lower(root.get("referencia")), "%" + referencia.trim().toLowerCase() + "%"));
        if(desde!=null)spec=spec.and((root,q,cb)->cb.greaterThanOrEqualTo(root.get("fechaEvento"),desde.atStartOfDay()));
        if(hasta!=null)spec=spec.and((root,q,cb)->cb.lessThan(root.get("fechaEvento"),hasta.plusDays(1).atStartOfDay()));
        return repository.findAll(spec,PageRequest.of(Math.max(0,page),Math.min(Math.max(size,1),100), Sort.by(Sort.Direction.DESC,"fechaEvento","id"))).map(this::response);
    }
    private AuditResponse response(AuditoriaOperacion e){return new AuditResponse(e.getId(),mapper.user(e.getUsuario()),e.getRolNombre(),e.getModulo(),e.getAccion(),e.getEntidadTipo(),e.getEntidadId(),e.getReferencia(),e.getDescripcion(),e.getMotivo(),e.getValoresAnteriores(),e.getValoresPosteriores(),e.getIpOrigen(),e.getAgenteUsuario(),e.getFechaEvento());}
    private boolean has(String v){return v!=null&&!v.isBlank();}
}
