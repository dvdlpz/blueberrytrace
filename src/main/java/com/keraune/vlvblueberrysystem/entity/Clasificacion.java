package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "clasificaciones")
public class Clasificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "cama_id", nullable = false)
    private Cama cama;

    @Column(name = "fecha_clasificacion", nullable = false)
    private LocalDate fechaClasificacion;

    @Column(name = "estado_planta", nullable = false, length = 60)
    private String estadoPlanta;

    @Column(nullable = false, length = 60)
    private String tamano;

    @Column(nullable = false, length = 120)
    private String condicion;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(length = 255)
    private String observacion;

    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";

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

    public Clasificacion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }
    public Cama getCama() { return cama; }
    public void setCama(Cama cama) { this.cama = cama; }
    public LocalDate getFechaClasificacion() { return fechaClasificacion; }
    public void setFechaClasificacion(LocalDate fechaClasificacion) { this.fechaClasificacion = fechaClasificacion; }
    public String getEstadoPlanta() { return estadoPlanta; }
    public void setEstadoPlanta(String estadoPlanta) { this.estadoPlanta = estadoPlanta; }
    public String getTamano() { return tamano; }
    public void setTamano(String tamano) { this.tamano = tamano; }
    public String getCondicion() { return condicion; }
    public void setCondicion(String condicion) { this.condicion = condicion; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
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
