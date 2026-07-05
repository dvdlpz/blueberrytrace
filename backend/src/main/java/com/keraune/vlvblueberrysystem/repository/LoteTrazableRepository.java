package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.LoteTrazable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoteTrazableRepository extends JpaRepository<LoteTrazable, Long> {
    List<LoteTrazable> findAllByOrderByFechaIngresoDescIdDesc();
    boolean existsByCodigoIgnoreCase(String codigo);
    List<LoteTrazable> findByEstadoIgnoreCaseOrderByCodigoAsc(String estado);
    List<LoteTrazable> findByEstadoIgnoreCaseOrderByFechaIngresoDesc(String estado);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select trace from LoteTrazable trace where trace.id = :id")
    Optional<LoteTrazable> findByIdForUpdate(@Param("id") Long id);
}
