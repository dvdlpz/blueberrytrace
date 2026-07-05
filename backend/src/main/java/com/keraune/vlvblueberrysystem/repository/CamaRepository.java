package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.Cama;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CamaRepository extends JpaRepository<Cama, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entity from Cama entity where entity.id = :id")
    Optional<Cama> findByIdForUpdate(@Param("id") Long id);
    List<Cama> findAllByOrderByCodigoAsc();
    long countByEstadoIgnoreCase(String estado);
    boolean existsByCodigoIgnoreCase(String codigo);
    long countByLoteId(Long loteId);
}
