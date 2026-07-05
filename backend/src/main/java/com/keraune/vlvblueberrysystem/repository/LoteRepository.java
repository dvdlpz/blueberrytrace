package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.Lote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entity from Lote entity where entity.id = :id")
    Optional<Lote> findByIdForUpdate(@Param("id") Long id);
    List<Lote> findAllByOrderByFechaRegistroDescIdDesc();
    long countByEstadoIgnoreCase(String estado);
    boolean existsByCodigoIgnoreCase(String codigo);
}
