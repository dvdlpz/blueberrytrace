package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.Auditoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    List<Auditoria> findTop50ByOrderByFechaHoraDesc();
}
