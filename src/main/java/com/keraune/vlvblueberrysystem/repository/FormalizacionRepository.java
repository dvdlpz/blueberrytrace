package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.Formalizacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FormalizacionRepository extends JpaRepository<Formalizacion, Long> {
    List<Formalizacion> findAllByOrderByFechaFormalizacionDescIdDesc();
    List<Formalizacion> findByLoteIdOrderByFechaFormalizacionDescIdDesc(Long loteId);
    long countByLoteId(Long loteId);
    long countByEstadoIgnoreCase(String estado);

    @Query("select coalesce(sum(f.cantidadPlantas), 0) from Formalizacion f")
    long sumCantidadPlantas();
}
