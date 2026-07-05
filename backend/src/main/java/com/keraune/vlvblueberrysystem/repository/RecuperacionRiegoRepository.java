package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.RecuperacionRiego;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecuperacionRiegoRepository extends JpaRepository<RecuperacionRiego, Long> {
    List<RecuperacionRiego> findAllByOrderByFechaIngresoRiegoDescIdDesc();
    List<RecuperacionRiego> findByLoteTrazableId(Long loteTrazableId);
}
