package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class FormalizacionForm {

    @NotNull(message = "Debe seleccionar un invernadero.")
    private Long loteId;

    @NotNull(message = "Debe seleccionar una cama.")
    private Long camaId;

    @NotNull(message = "La fecha de formalización es obligatoria.")
    private LocalDate fechaFormalizacion;

    @NotBlank(message = "El detalle de formalización es obligatorio.")
    @Size(max = 180, message = "El detalle no puede superar los 180 caracteres.")
    private String detalle;

    @NotNull(message = "La cantidad de bandejas es obligatoria.")
    @Min(value = 0, message = "La cantidad de bandejas no puede ser negativa.")
    private Integer cantidadBandejas;

    @NotNull(message = "La cantidad de plantas es obligatoria.")
    @Min(value = 0, message = "La cantidad de plantas no puede ser negativa.")
    private Integer cantidadPlantas;

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres.")
    private String observacion;

    @Size(max = 30, message = "El estado no puede superar los 30 caracteres.")
    private String estado = "REGISTRADA";

    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }
    public Long getCamaId() { return camaId; }
    public void setCamaId(Long camaId) { this.camaId = camaId; }
    public LocalDate getFechaFormalizacion() { return fechaFormalizacion; }
    public void setFechaFormalizacion(LocalDate fechaFormalizacion) { this.fechaFormalizacion = fechaFormalizacion; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public Integer getCantidadBandejas() { return cantidadBandejas; }
    public void setCantidadBandejas(Integer cantidadBandejas) { this.cantidadBandejas = cantidadBandejas; }
    public Integer getCantidadPlantas() { return cantidadPlantas; }
    public void setCantidadPlantas(Integer cantidadPlantas) { this.cantidadPlantas = cantidadPlantas; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
