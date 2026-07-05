package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;

/** One requested blueberry variety inside a commercial order. */
@Entity
@Table(name = "pedido_detalles")
public class PedidoDetalle extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;
    @Column(nullable = false, length = 120)
    private String variedad;
    @Column(name = "cantidad_solicitada", nullable = false)
    private Integer cantidadSolicitada;
    @Column(length = 255)
    private String observacion;

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public String getVariedad() { return variedad; }
    public void setVariedad(String variedad) { this.variedad = variedad; }
    public Integer getCantidadSolicitada() { return cantidadSolicitada; }
    public void setCantidadSolicitada(Integer cantidadSolicitada) { this.cantidadSolicitada = cantidadSolicitada; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
