package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.Clasificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClasificacionRepository extends JpaRepository<Clasificacion, Long> {
    List<Clasificacion> findAllByOrderByFechaClasificacionDescIdDesc();
    long countByEstadoIgnoreCase(String estado);
    long countByLoteId(Long loteId);
    List<Clasificacion> findByCamaId(Long camaId);
    List<Clasificacion> findByJabaId(Long jabaId);
    List<Clasificacion> findByLoteId(Long loteId);
    List<Clasificacion> findByLoteTrazableId(Long loteTrazableId);
    List<Clasificacion> findByLoteIdAndCamaIdAndLoteTrazableIsNull(Long loteId, Long camaId);
}
