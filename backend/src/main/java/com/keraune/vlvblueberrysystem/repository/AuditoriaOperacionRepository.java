package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.AuditoriaOperacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditoriaOperacionRepository extends JpaRepository<AuditoriaOperacion, Long>, JpaSpecificationExecutor<AuditoriaOperacion> {
    Page<AuditoriaOperacion> findAllByOrderByFechaEventoDescIdDesc(Pageable pageable);
}
