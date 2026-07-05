package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "uniformizaciones")
public class Uniformizacion extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_trazable_id")
    private LoteTrazable loteTrazable;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "cama_id", nullable = false)
    private Cama cama;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "jaba_origen_id")
    private Jaba jabaOrigen;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "jaba_destino_id")
    private Jaba jabaDestino;
    @Column(name = "fecha_uniformizacion", nullable = false)
    private LocalDate fechaUniformizacion;
    @Column(nullable = false, length = 120)
    private String criterio;
    @Column(name = "cantidad_inicial", nullable = false)
    private Integer cantidadInicial;
    @Column(name = "cantidad_uniformizada", nullable = false)
    private Integer cantidadUniformizada;
    @Column(name = "origen_operativo", nullable = false, length = 40)
    private String origenOperativo = "SIEMBRA";
    @Column(name = "cantidad_recuperacion", nullable = false)
    private Integer cantidadRecuperacion = 0;
    @Column(name = "malezas_retiradas", nullable = false)
    private Boolean malezasRetiradas = false;
    @Column(length = 255)
    private String observacion;
    @Column(nullable = false, length = 30)
    private String estado = "REGISTRADA";
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recuperacion_riego_id")
    private RecuperacionRiego recuperacionRiego;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_registro_id", nullable = false)
    private User usuarioRegistro;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }
    public LoteTrazable getLoteTrazable() { return loteTrazable; }
    public void setLoteTrazable(LoteTrazable loteTrazable) { this.loteTrazable = loteTrazable; }
    public Cama getCama() { return cama; }
    public void setCama(Cama cama) { this.cama = cama; }
    public Jaba getJabaOrigen() { return jabaOrigen; }
    public void setJabaOrigen(Jaba jabaOrigen) { this.jabaOrigen = jabaOrigen; }
    public Jaba getJabaDestino() { return jabaDestino; }
    public void setJabaDestino(Jaba jabaDestino) { this.jabaDestino = jabaDestino; }
    public LocalDate getFechaUniformizacion() { return fechaUniformizacion; }
    public void setFechaUniformizacion(LocalDate fechaUniformizacion) { this.fechaUniformizacion = fechaUniformizacion; }
    public String getCriterio() { return criterio; }
    public void setCriterio(String criterio) { this.criterio = criterio; }
    public Integer getCantidadInicial() { return cantidadInicial; }
    public void setCantidadInicial(Integer cantidadInicial) { this.cantidadInicial = cantidadInicial; }
    public Integer getCantidadUniformizada() { return cantidadUniformizada; }
    public void setCantidadUniformizada(Integer cantidadUniformizada) { this.cantidadUniformizada = cantidadUniformizada; }
    public String getOrigenOperativo() { return origenOperativo; }
    public void setOrigenOperativo(String origenOperativo) { this.origenOperativo = origenOperativo; }
    public Integer getCantidadRecuperacion() { return cantidadRecuperacion; }
    public void setCantidadRecuperacion(Integer cantidadRecuperacion) { this.cantidadRecuperacion = cantidadRecuperacion; }
    public Boolean getMalezasRetiradas() { return malezasRetiradas; }
    public void setMalezasRetiradas(Boolean malezasRetiradas) { this.malezasRetiradas = malezasRetiradas; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public RecuperacionRiego getRecuperacionRiego() { return recuperacionRiego; }
    public void setRecuperacionRiego(RecuperacionRiego recuperacionRiego) { this.recuperacionRiego = recuperacionRiego; }
    public User getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(User usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
