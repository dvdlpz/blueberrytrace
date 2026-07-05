package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.Jaba;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JabaRepository extends JpaRepository<Jaba, Long> {
    List<Jaba> findAllByOrderByCodigoAsc();
    List<Jaba> findByCamaIdOrderByOrdenEnCamaAscCodigoAsc(Long camaId);
    boolean existsByCodigoIgnoreCase(String codigo);
}
