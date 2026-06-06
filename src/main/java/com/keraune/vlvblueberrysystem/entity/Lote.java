package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lotes")
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String descripcion;

    @Column(length = 120)
    private String cultivo;

    @Column(length = 120)
    private String variedad;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Column(length = 255)
    private String observacion;

    @Column(nullable = false, length = 30)
    private String estado = "ACTIVO";

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

    public Lote() {}

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getDescripcion() { return descripcion; }
    public String getCultivo() { return cultivo; }
    public String getVariedad() { return variedad; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public String getObservacion() { return observacion; }
    public String getEstado() { return estado; }
    public User getUsuarioRegistro() { return usuarioRegistro; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }

    public void setId(Long id) { this.id = id; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setCultivo(String cultivo) { this.cultivo = cultivo; }
    public void setVariedad(String variedad) { this.variedad = variedad; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setUsuarioRegistro(User usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
