package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "mermas")
public class Merma extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_trazable_id", nullable = false)
    private LoteTrazable loteTrazable;

    @Column(name = "etapa_origen", nullable = false, length = 40)
    private String etapaOrigen;

    @Column(nullable = false, length = 120)
    private String motivo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "fecha_merma", nullable = false)
    private LocalDate fechaMerma;

    @Column(length = 255)
    private String observacion;

    @Column(nullable = false, length = 30)
    private String estado = "REGISTRADA";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_registro_id", nullable = false)
    private User usuarioRegistro;

    public Long getId() { return id; }
    public LoteTrazable getLoteTrazable() { return loteTrazable; }
    public void setLoteTrazable(LoteTrazable loteTrazable) { this.loteTrazable = loteTrazable; }
    public String getEtapaOrigen() { return etapaOrigen; }
    public void setEtapaOrigen(String etapaOrigen) { this.etapaOrigen = etapaOrigen; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public LocalDate getFechaMerma() { return fechaMerma; }
    public void setFechaMerma(LocalDate fechaMerma) { this.fechaMerma = fechaMerma; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public User getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(User usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
