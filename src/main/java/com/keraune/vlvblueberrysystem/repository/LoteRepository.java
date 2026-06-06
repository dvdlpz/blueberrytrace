package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.Lote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoteRepository extends JpaRepository<Lote, Long> {
    Optional<Lote> findByCodigo(String codigo);
    boolean existsByCodigoIgnoreCase(String codigo);
    List<Lote> findAllByOrderByFechaRegistroDescIdDesc();
    long countByEstadoIgnoreCase(String estado);
}
