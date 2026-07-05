package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Dry or observed plants sent to programmed irrigation before returning to an operational stage. */
@Entity
@Table(name = "recuperaciones_riego")
public class RecuperacionRiego extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_trazable_id", nullable = false)
    private LoteTrazable loteTrazable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jaba_id")
    private Jaba jaba;

    @Column(name = "etapa_origen", nullable = false, length = 40)
    private String etapaOrigen;

    @Column(name = "etapa_retorno", nullable = false, length = 40)
    private String etapaRetorno;

    @Column(name = "fecha_ingreso_riego", nullable = false)
    private LocalDate fechaIngresoRiego;

    @Column(name = "cantidad_ingresada", nullable = false)
    private Integer cantidadIngresada;

    @Column(name = "cantidad_recuperada", nullable = false)
    private Integer cantidadRecuperada = 0;

    @Column(name = "cantidad_descartada", nullable = false)
    private Integer cantidadDescartada = 0;

    @Column(name = "motivo_descarte", length = 120)
    private String motivoDescarte;

    @Column(length = 255)
    private String observacion;

    @Column(nullable = false, length = 30)
    private String estado = "EN_RIEGO";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merma_generada_id")
    private Merma mermaGenerada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_registro_id", nullable = false)
    private User usuarioRegistro;

    public Long getId() { return id; }
    public LoteTrazable getLoteTrazable() { return loteTrazable; }
    public void setLoteTrazable(LoteTrazable loteTrazable) { this.loteTrazable = loteTrazable; }
    public Jaba getJaba() { return jaba; }
    public void setJaba(Jaba jaba) { this.jaba = jaba; }
    public String getEtapaOrigen() { return etapaOrigen; }
    public void setEtapaOrigen(String etapaOrigen) { this.etapaOrigen = etapaOrigen; }
    public String getEtapaRetorno() { return etapaRetorno; }
    public void setEtapaRetorno(String etapaRetorno) { this.etapaRetorno = etapaRetorno; }
    public LocalDate getFechaIngresoRiego() { return fechaIngresoRiego; }
    public void setFechaIngresoRiego(LocalDate fechaIngresoRiego) { this.fechaIngresoRiego = fechaIngresoRiego; }
    public Integer getCantidadIngresada() { return cantidadIngresada; }
    public void setCantidadIngresada(Integer cantidadIngresada) { this.cantidadIngresada = cantidadIngresada; }
    public Integer getCantidadRecuperada() { return cantidadRecuperada; }
    public void setCantidadRecuperada(Integer cantidadRecuperada) { this.cantidadRecuperada = cantidadRecuperada; }
    public Integer getCantidadDescartada() { return cantidadDescartada; }
    public void setCantidadDescartada(Integer cantidadDescartada) { this.cantidadDescartada = cantidadDescartada; }
    public String getMotivoDescarte() { return motivoDescarte; }
    public void setMotivoDescarte(String motivoDescarte) { this.motivoDescarte = motivoDescarte; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Merma getMermaGenerada() { return mermaGenerada; }
    public void setMermaGenerada(Merma mermaGenerada) { this.mermaGenerada = mermaGenerada; }
    public User getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(User usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
