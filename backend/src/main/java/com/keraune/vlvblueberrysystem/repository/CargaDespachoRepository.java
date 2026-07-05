package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.CargaDespacho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CargaDespachoRepository extends JpaRepository<CargaDespacho, Long> {
    List<CargaDespacho> findAllByOrderByFechaCargaDescIdDesc();
    boolean existsByCodigoIgnoreCase(String codigo);
}
