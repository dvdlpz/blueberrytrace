package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CamaForm {

    @NotBlank(message = "El código es obligatorio.")
    @Size(max = 30, message = "El código no puede superar los 30 caracteres.")
    private String codigo;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 150, message = "La descripción no puede superar los 150 caracteres.")
    private String descripcion;

    @NotNull(message = "La capacidad referencial es obligatoria.")
    @Min(value = 1, message = "La capacidad referencial debe ser mayor a 0.")
    private Integer capacidadReferencial;

    @NotBlank(message = "El estado es obligatorio.")
    @Size(max = 30, message = "El estado no puede superar los 30 caracteres.")
    private String estado = "ACTIVA";

    @NotNull(message = "Debe seleccionar un invernadero.")
    private Long loteId;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCapacidadReferencial() {
        return capacidadReferencial;
    }

    public void setCapacidadReferencial(Integer capacidadReferencial) {
        this.capacidadReferencial = capacidadReferencial;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Long getLoteId() {
        return loteId;
    }

    public void setLoteId(Long loteId) {
        this.loteId = loteId;
    }
}
