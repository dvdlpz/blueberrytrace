package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "camas")
public class Cama {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String descripcion;

    @Column(name = "capacidad_referencial", nullable = false)
    private Integer capacidadReferencial;

    @Column(nullable = false, length = 30)
    private String estado = "ACTIVA";

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_registro_id", nullable = false)
    private User usuarioRegistro;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void prePersist() {
        this.fechaCreacion = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    public Cama() {}

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
    public Integer getCapacidadReferencial() { return capacidadReferencial; }
    public String getEstado() { return estado; }
    public Lote getLote() { return lote; }
    public User getUsuarioRegistro() { return usuarioRegistro; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }

    public void setId(Long id) { this.id = id; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setCapacidadReferencial(Integer capacidadReferencial) { this.capacidadReferencial = capacidadReferencial; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setLote(Lote lote) { this.lote = lote; }
    public void setUsuarioRegistro(User usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
