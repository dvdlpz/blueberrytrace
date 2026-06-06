package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.Clasificacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClasificacionRepository extends JpaRepository<Clasificacion, Long> {
    List<Clasificacion> findAllByOrderByFechaClasificacionDescIdDesc();
    List<Clasificacion> findByLoteIdOrderByFechaClasificacionDescIdDesc(Long loteId);
    long countByLoteId(Long loteId);
    long countByEstadoIgnoreCase(String estado);

    @Query("select coalesce(sum(c.cantidad), 0) from Clasificacion c")
    long sumCantidadClasificada();
}
