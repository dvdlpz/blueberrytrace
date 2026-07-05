package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "lotes_trazables")
public class LoteTrazable extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String variedad;

    @Column(nullable = false, length = 180)
    private String procedencia;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(nullable = false, length = 30)
    private String estado = "ACTIVO";

    @Column(length = 255)
    private String observacion;

    @Column(name = "legado_pendiente_normalizacion", nullable = false)
    private Boolean legadoPendienteNormalizacion = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_fisico_id", nullable = false)
    private Lote loteFisico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cama_inicial_id", nullable = false)
    private Cama camaInicial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_responsable_id", nullable = false)
    private User usuarioResponsable;

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getVariedad() { return variedad; }
    public void setVariedad(String variedad) { this.variedad = variedad; }
    public String getProcedencia() { return procedencia; }
    public void setProcedencia(String procedencia) { this.procedencia = procedencia; }
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate fechaIngreso) { this.fechaIngreso = fechaIngreso; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public Boolean getLegadoPendienteNormalizacion() { return legadoPendienteNormalizacion; }
    public void setLegadoPendienteNormalizacion(Boolean legadoPendienteNormalizacion) { this.legadoPendienteNormalizacion = legadoPendienteNormalizacion; }
    public Lote getLoteFisico() { return loteFisico; }
    public void setLoteFisico(Lote loteFisico) { this.loteFisico = loteFisico; }
    public Cama getCamaInicial() { return camaInicial; }
    public void setCamaInicial(Cama camaInicial) { this.camaInicial = camaInicial; }
    public User getUsuarioResponsable() { return usuarioResponsable; }
    public void setUsuarioResponsable(User usuarioResponsable) { this.usuarioResponsable = usuarioResponsable; }
}
