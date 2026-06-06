package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "despachos")
public class Despacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;

    @Column(name = "fecha_despacho", nullable = false)
    private LocalDate fechaDespacho;

    @Column(name = "modalidad_despacho", nullable = false, length = 80)
    private String modalidad;

    /**
     * Compatibility column for databases that were updated with an older entity mapping.
     * It receives the same value as modalidad to avoid NOT NULL failures on existing schemas.
     */
    @Column(name = "modalidad", length = 80)
    private String modalidadCompat;

    @Column(name = "cantidad_despachada", nullable = false)
    private Integer cantidadDespachada;

    /**
     * Compatibility column for databases that were created with an older despacho mapping.
     * Some local schemas still keep this column as NOT NULL, so it must receive the
     * same value as cantidadDespachada on every insert/update.
     */
    @Column(name = "cantidad")
    private Integer cantidadCompat;

    @Column(length = 120)
    private String destino;

    @Column(name = "guia_remision", length = 80)
    private String guiaRemision;

    @Column(name = "validacion_calidad", nullable = false, length = 120)
    private String validacionCalidad;

    @Column(length = 255)
    private String observacion;

    @Column(nullable = false, length = 30)
    private String estado = "REGISTRADO";

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
        syncCompatibilityColumns();
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
        syncCompatibilityColumns();
    }

    private void syncCompatibilityColumns() {
        this.modalidadCompat = this.modalidad;
        this.cantidadCompat = this.cantidadDespachada;
    }

    public Despacho() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }
    public LocalDate getFechaDespacho() { return fechaDespacho; }
    public void setFechaDespacho(LocalDate fechaDespacho) { this.fechaDespacho = fechaDespacho; }
    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
        this.modalidadCompat = modalidad;
    }
    public Integer getCantidadDespachada() { return cantidadDespachada; }
    public void setCantidadDespachada(Integer cantidadDespachada) {
        this.cantidadDespachada = cantidadDespachada;
        this.cantidadCompat = cantidadDespachada;
    }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getGuiaRemision() { return guiaRemision; }
    public void setGuiaRemision(String guiaRemision) { this.guiaRemision = guiaRemision; }
    public String getValidacionCalidad() { return validacionCalidad; }
    public void setValidacionCalidad(String validacionCalidad) { this.validacionCalidad = validacionCalidad; }
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
