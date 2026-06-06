package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.Uniformizacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UniformizacionRepository extends JpaRepository<Uniformizacion, Long> {
    List<Uniformizacion> findAllByOrderByFechaUniformizacionDescIdDesc();
    List<Uniformizacion> findByLoteIdOrderByFechaUniformizacionDescIdDesc(Long loteId);
    long countByLoteId(Long loteId);
    long countByEstadoIgnoreCase(String estado);

    @Query("select coalesce(sum(u.cantidadUniformizada), 0) from Uniformizacion u")
    long sumCantidadUniformizada();
}
