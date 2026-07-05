package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.Empaque;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmpaqueRepository extends JpaRepository<Empaque, Long> {
    List<Empaque> findAllByOrderByFechaEmpaqueDescIdDesc();
    List<Empaque> findByClasificacionId(Long clasificacionId);
    List<Empaque> findByPedidoDetalleId(Long pedidoDetalleId);
    List<Empaque> findByLoteTrazableId(Long loteTrazableId);
}
