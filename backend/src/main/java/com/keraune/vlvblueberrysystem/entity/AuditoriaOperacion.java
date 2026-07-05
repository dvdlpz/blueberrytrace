package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_operaciones")
public class AuditoriaOperacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User usuario;

    @Column(name = "rol_nombre", length = 50)
    private String rolNombre;

    @Column(nullable = false, length = 80)
    private String modulo;

    @Column(nullable = false, length = 80)
    private String accion;

    @Column(name = "entidad_tipo", length = 80)
    private String entidadTipo;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "referencia", length = 160)
    private String referencia;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @Lob
    @Column(name = "valores_anteriores", columnDefinition = "LONGTEXT")
    private String valoresAnteriores;

    @Lob
    @Column(name = "valores_posteriores", columnDefinition = "LONGTEXT")
    private String valoresPosteriores;

    @Column(name = "ip_origen", length = 64)
    private String ipOrigen;

    @Column(name = "agente_usuario", length = 500)
    private String agenteUsuario;

    @Column(name = "fecha_evento", nullable = false, updatable = false)
    private LocalDateTime fechaEvento;

    @PrePersist
    void onCreate() {
        if (fechaEvento == null) {
            fechaEvento = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public User getUsuario() { return usuario; }
    public void setUsuario(User usuario) { this.usuario = usuario; }
    public String getRolNombre() { return rolNombre; }
    public void setRolNombre(String rolNombre) { this.rolNombre = rolNombre; }
    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }
    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
    public String getEntidadTipo() { return entidadTipo; }
    public void setEntidadTipo(String entidadTipo) { this.entidadTipo = entidadTipo; }
    public Long getEntidadId() { return entidadId; }
    public void setEntidadId(Long entidadId) { this.entidadId = entidadId; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getValoresAnteriores() { return valoresAnteriores; }
    public void setValoresAnteriores(String valoresAnteriores) { this.valoresAnteriores = valoresAnteriores; }
    public String getValoresPosteriores() { return valoresPosteriores; }
    public void setValoresPosteriores(String valoresPosteriores) { this.valoresPosteriores = valoresPosteriores; }
    public String getIpOrigen() { return ipOrigen; }
    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }
    public String getAgenteUsuario() { return agenteUsuario; }
    public void setAgenteUsuario(String agenteUsuario) { this.agenteUsuario = agenteUsuario; }
    public LocalDateTime getFechaEvento() { return fechaEvento; }
}
