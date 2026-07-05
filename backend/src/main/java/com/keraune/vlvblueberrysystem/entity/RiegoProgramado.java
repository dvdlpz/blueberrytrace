package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

/** Planned irrigation at bed level, optionally focused on one propagation crate. */
@Entity
@Table(name = "riegos_programados")
public class RiegoProgramado extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_trazable_id", nullable = false)
    private LoteTrazable loteTrazable;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cama_id", nullable = false)
    private Cama cama;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jaba_id")
    private Jaba jaba;

    @Column(name = "fecha_programada", nullable = false)
    private LocalDate fechaProgramada;

    @Column(name = "hora_programada", nullable = false)
    private LocalTime horaProgramada;

    @Column(name = "fecha_ejecucion")
    private LocalDate fechaEjecucion;

    @Column(name = "hora_ejecucion")
    private LocalTime horaEjecucion;

    @Column(name = "etapa_aplicacion", nullable = false, length = 40)
    private String etapaAplicacion;

    @Column(nullable = false, length = 30)
    private String estado = "PROGRAMADO";

    @Column(length = 255)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_registro_id", nullable = false)
    private User usuarioRegistro;

    public Long getId() { return id; }
    public LoteTrazable getLoteTrazable() { return loteTrazable; }
    public void setLoteTrazable(LoteTrazable loteTrazable) { this.loteTrazable = loteTrazable; }
    public Cama getCama() { return cama; }
    public void setCama(Cama cama) { this.cama = cama; }
    public Jaba getJaba() { return jaba; }
    public void setJaba(Jaba jaba) { this.jaba = jaba; }
    public LocalDate getFechaProgramada() { return fechaProgramada; }
    public void setFechaProgramada(LocalDate fechaProgramada) { this.fechaProgramada = fechaProgramada; }
    public LocalTime getHoraProgramada() { return horaProgramada; }
    public void setHoraProgramada(LocalTime horaProgramada) { this.horaProgramada = horaProgramada; }
    public LocalDate getFechaEjecucion() { return fechaEjecucion; }
    public void setFechaEjecucion(LocalDate fechaEjecucion) { this.fechaEjecucion = fechaEjecucion; }
    public LocalTime getHoraEjecucion() { return horaEjecucion; }
    public void setHoraEjecucion(LocalTime horaEjecucion) { this.horaEjecucion = horaEjecucion; }
    public String getEtapaAplicacion() { return etapaAplicacion; }
    public void setEtapaAplicacion(String etapaAplicacion) { this.etapaAplicacion = etapaAplicacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public User getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(User usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
