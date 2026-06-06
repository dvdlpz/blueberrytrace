package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.Despacho;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DespachoRepository extends JpaRepository<Despacho, Long> {
    List<Despacho> findAllByOrderByFechaDespachoDescIdDesc();
    List<Despacho> findByLoteIdOrderByFechaDespachoDescIdDesc(Long loteId);
    long countByLoteId(Long loteId);
    long countByEstadoIgnoreCase(String estado);

    @Query("select coalesce(sum(d.cantidadDespachada), 0) from Despacho d")
    long sumCantidadDespachada();

    @Query("select coalesce(sum(d.cantidadDespachada), 0) from Despacho d where d.lote.id = :loteId")
    long sumCantidadDespachadaByLoteId(@Param("loteId") Long loteId);
}
