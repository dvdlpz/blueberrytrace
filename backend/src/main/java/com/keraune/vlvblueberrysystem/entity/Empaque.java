package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Prepared units before dispatch: a 15-pot cosechera crate or a configured wooden bin. */
@Entity
@Table(name = "empaques")
public class Empaque extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_trazable_id", nullable = false)
    private LoteTrazable loteTrazable;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clasificacion_id", nullable = false)
    private Clasificacion clasificacion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_detalle_id", nullable = false)
    private PedidoDetalle pedidoDetalle;
    @Column(nullable = false, length = 40)
    private String tipo;
    @Column(name = "capacidad_por_unidad", nullable = false)
    private Integer capacidadPorUnidad;
    @Column(name = "cantidad_unidades", nullable = false)
    private Integer cantidadUnidades;
    @Column(name = "cantidad_plantas", nullable = false)
    private Integer cantidadPlantas;
    @Column(name = "fecha_empaque", nullable = false)
    private LocalDate fechaEmpaque;
    @Column(nullable = false, length = 30)
    private String estado = "PREPARADO";
    @Column(length = 255)
    private String observacion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_registro_id", nullable = false)
    private User usuarioRegistro;

    public Long getId() { return id; }
    public LoteTrazable getLoteTrazable() { return loteTrazable; }
    public void setLoteTrazable(LoteTrazable loteTrazable) { this.loteTrazable = loteTrazable; }
    public Clasificacion getClasificacion() { return clasificacion; }
    public void setClasificacion(Clasificacion clasificacion) { this.clasificacion = clasificacion; }
    public PedidoDetalle getPedidoDetalle() { return pedidoDetalle; }
    public void setPedidoDetalle(PedidoDetalle pedidoDetalle) { this.pedidoDetalle = pedidoDetalle; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getCapacidadPorUnidad() { return capacidadPorUnidad; }
    public void setCapacidadPorUnidad(Integer capacidadPorUnidad) { this.capacidadPorUnidad = capacidadPorUnidad; }
    public Integer getCantidadUnidades() { return cantidadUnidades; }
    public void setCantidadUnidades(Integer cantidadUnidades) { this.cantidadUnidades = cantidadUnidades; }
    public Integer getCantidadPlantas() { return cantidadPlantas; }
    public void setCantidadPlantas(Integer cantidadPlantas) { this.cantidadPlantas = cantidadPlantas; }
    public LocalDate getFechaEmpaque() { return fechaEmpaque; }
    public void setFechaEmpaque(LocalDate fechaEmpaque) { this.fechaEmpaque = fechaEmpaque; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public User getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(User usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
