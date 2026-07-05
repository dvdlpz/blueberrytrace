package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.RiegoProgramado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiegoProgramadoRepository extends JpaRepository<RiegoProgramado, Long> {
    List<RiegoProgramado> findAllByOrderByFechaProgramadaDescHoraProgramadaDescIdDesc();
    List<RiegoProgramado> findByLoteTrazableId(Long loteTrazableId);
}
