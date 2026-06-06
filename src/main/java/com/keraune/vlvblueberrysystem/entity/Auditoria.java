package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditorias")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User usuario;

    @Column(nullable = false, length = 80)
    private String modulo;

    @Column(nullable = false, length = 120)
    private String accion;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @PrePersist
    public void prePersist() {
        if (this.fechaHora == null) {
            this.fechaHora = LocalDateTime.now();
        }
    }

    public Auditoria() {}

    public Auditoria(User usuario, String modulo, String accion, String descripcion) {
        this.usuario = usuario;
        this.modulo = modulo;
        this.accion = accion;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public User getUsuario() { return usuario; }
    public String getModulo() { return modulo; }
    public String getAccion() { return accion; }
    public String getDescripcion() { return descripcion; }
    public LocalDateTime getFechaHora() { return fechaHora; }

    public void setId(Long id) { this.id = id; }
    public void setUsuario(User usuario) { this.usuario = usuario; }
    public void setModulo(String modulo) { this.modulo = modulo; }
    public void setAccion(String accion) { this.accion = accion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
}
