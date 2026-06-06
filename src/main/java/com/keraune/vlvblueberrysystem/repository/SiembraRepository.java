package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.Siembra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SiembraRepository extends JpaRepository<Siembra, Long> {
    List<Siembra> findAllByOrderByFechaSiembraDescIdDesc();
    List<Siembra> findByLoteIdOrderByFechaSiembraDescIdDesc(Long loteId);
    long countByLoteId(Long loteId);
    long countByEstadoIgnoreCase(String estado);

    @Query("select coalesce(sum(s.cantidadRegistrada), 0) from Siembra s")
    long sumCantidadRegistrada();

    @Query("select coalesce(sum(s.cantidadRegistrada), 0) from Siembra s where s.lote.id = :loteId")
    long sumCantidadRegistradaByLoteId(@Param("loteId") Long loteId);
}
