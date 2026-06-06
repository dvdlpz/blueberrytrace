package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "siembras")
public class Siembra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "cama_id", nullable = false)
    private Cama cama;

    @Column(name = "fecha_siembra", nullable = false)
    private LocalDate fechaSiembra;

    @Column(name = "cantidad_registrada", nullable = false)
    private Integer cantidadRegistrada;

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

    public Siembra() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }
    public Cama getCama() { return cama; }
    public void setCama(Cama cama) { this.cama = cama; }
    public LocalDate getFechaSiembra() { return fechaSiembra; }
    public void setFechaSiembra(LocalDate fechaSiembra) { this.fechaSiembra = fechaSiembra; }
    public Integer getCantidadRegistrada() { return cantidadRegistrada; }
    public void setCantidadRegistrada(Integer cantidadRegistrada) { this.cantidadRegistrada = cantidadRegistrada; }
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
