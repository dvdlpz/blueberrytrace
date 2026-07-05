package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;

/** Physical propagation crate that belongs to a productive bed. */
@Entity
@Table(name = "jabas")
public class Jaba extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cama_id", nullable = false)
    private Cama cama;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(name = "capacidad_macetas", nullable = false)
    private Integer capacidadMacetas;

    @Column(name = "orden_en_cama")
    private Integer ordenEnCama;

    @Column(nullable = false, length = 30)
    private String estado = "ACTIVA";

    @Column(length = 255)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_registro_id", nullable = false)
    private User usuarioRegistro;

    public Long getId() { return id; }
    public Cama getCama() { return cama; }
    public void setCama(Cama cama) { this.cama = cama; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public Integer getCapacidadMacetas() { return capacidadMacetas; }
    public void setCapacidadMacetas(Integer capacidadMacetas) { this.capacidadMacetas = capacidadMacetas; }
    public Integer getOrdenEnCama() { return ordenEnCama; }
    public void setOrdenEnCama(Integer ordenEnCama) { this.ordenEnCama = ordenEnCama; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public User getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(User usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
