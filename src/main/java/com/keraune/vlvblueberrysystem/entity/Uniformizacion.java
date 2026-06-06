package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "uniformizaciones")
public class Uniformizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "cama_id", nullable = false)
    private Cama cama;

    @Column(name = "fecha_uniformizacion", nullable = false)
    private LocalDate fechaUniformizacion;

    @Column(nullable = false, length = 120)
    private String criterio;

    @Column(name = "cantidad_inicial", nullable = false)
    private Integer cantidadInicial;

    @Column(name = "cantidad_uniformizada", nullable = false)
    private Integer cantidadUniformizada;

    @Column(length = 255)
    private String observacion;

    @Column(nullable = false, length = 30)
    private String estado = "REGISTRADA";

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_registro_id", nullable = false)
    private User usuarioRegistro;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void prePersist() { this.fechaCreacion = LocalDateTime.now(); }

    @PreUpdate
    public void preUpdate() { this.fechaActualizacion = LocalDateTime.now(); }

    public Uniformizacion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }
    public Cama getCama() { return cama; }
    public void setCama(Cama cama) { this.cama = cama; }
    public LocalDate getFechaUniformizacion() { return fechaUniformizacion; }
    public void setFechaUniformizacion(LocalDate fechaUniformizacion) { this.fechaUniformizacion = fechaUniformizacion; }
    public String getCriterio() { return criterio; }
    public void setCriterio(String criterio) { this.criterio = criterio; }
    public Integer getCantidadInicial() { return cantidadInicial; }
    public void setCantidadInicial(Integer cantidadInicial) { this.cantidadInicial = cantidadInicial; }
    public Integer getCantidadUniformizada() { return cantidadUniformizada; }
    public void setCantidadUniformizada(Integer cantidadUniformizada) { this.cantidadUniformizada = cantidadUniformizada; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public User getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(User usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
