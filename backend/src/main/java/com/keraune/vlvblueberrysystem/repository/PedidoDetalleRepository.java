package com.keraune.vlvblueberrysystem.repository;

import com.keraune.vlvblueberrysystem.entity.PedidoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoDetalleRepository extends JpaRepository<PedidoDetalle, Long> {
    List<PedidoDetalle> findByPedidoIdOrderByVariedadAsc(Long pedidoId);
}
