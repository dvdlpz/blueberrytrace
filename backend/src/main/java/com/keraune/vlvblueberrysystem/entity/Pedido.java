package com.keraune.vlvblueberrysystem.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Commercial request that controls quantities requested by variety. */
@Entity
@Table(name = "pedidos")
public class Pedido extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;
    @Column(nullable = false, length = 150)
    private String cliente;
    @Column(length = 160)
    private String destino;
    @Column(name = "fecha_compromiso", nullable = false)
    private LocalDate fechaCompromiso;
    @Column(nullable = false, length = 30)
    private String estado = "BORRADOR";
    @Column(length = 255)
    private String observacion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_registro_id", nullable = false)
    private User usuarioRegistro;

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public LocalDate getFechaCompromiso() { return fechaCompromiso; }
    public void setFechaCompromiso(LocalDate fechaCompromiso) { this.fechaCompromiso = fechaCompromiso; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public User getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(User usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
