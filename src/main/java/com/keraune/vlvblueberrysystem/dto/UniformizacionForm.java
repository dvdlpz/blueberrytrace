package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class UniformizacionForm {

    @NotNull(message = "Debe seleccionar un invernadero.")
    private Long loteId;

    @NotNull(message = "Debe seleccionar una cama.")
    private Long camaId;

    @NotNull(message = "La fecha de uniformización es obligatoria.")
    private LocalDate fechaUniformizacion;

    @NotBlank(message = "El criterio de uniformización es obligatorio.")
    @Size(max = 120, message = "El criterio no puede superar los 120 caracteres.")
    private String criterio;

    @NotNull(message = "La cantidad inicial es obligatoria.")
    @Min(value = 0, message = "La cantidad inicial no puede ser negativa.")
    private Integer cantidadInicial;

    @NotNull(message = "La cantidad uniformizada es obligatoria.")
    @Min(value = 0, message = "La cantidad uniformizada no puede ser negativa.")
    private Integer cantidadUniformizada;

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres.")
    private String observacion;

    @Size(max = 30, message = "El estado no puede superar los 30 caracteres.")
    private String estado = "REGISTRADA";

    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }
    public Long getCamaId() { return camaId; }
    public void setCamaId(Long camaId) { this.camaId = camaId; }
    public LocalDate getFechaUniformizacion() { return fechaUniformizacion; }
    public void setFechaUniformizacion(LocalDate fechaUniformizacion) { this.fechaUniformizacion = fechaUniformizacion; }
    public String getCriterio() { return criterio; }
    public void setCriterio(String criterio) { this.criterio = criterio; }
    public Integer getCantidadInicial() { return cantidadInicial; }
    public void setCantidadInicial(Integer cantidadInicial) { this.cantidadInicial = cantidadInicial; }
    public Integer getCantidadUniformizada() { return cantidadUniformizada; }
    public void setCantidadUniformizada(Integer cantidadUniformizada) { this.cantidadUniformizada = cantidadUniformizada; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
