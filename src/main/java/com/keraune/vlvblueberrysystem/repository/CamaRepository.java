package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.Cama;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CamaRepository extends JpaRepository<Cama, Long> {
    Optional<Cama> findByCodigo(String codigo);
    List<Cama> findAllByOrderByCodigoAsc();
    long countByEstadoIgnoreCase(String estado);
    long countByLoteId(Long loteId);

    @Query("select coalesce(sum(c.capacidadReferencial), 0) from Cama c")
    long sumCapacidadReferencial();
}
