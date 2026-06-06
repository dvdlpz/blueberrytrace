package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class DespachoForm {

    @NotNull(message = "Debe seleccionar un invernadero.")
    private Long loteId;

    @NotNull(message = "La fecha de despacho es obligatoria.")
    private LocalDate fechaDespacho;

    @NotBlank(message = "La modalidad de despacho es obligatoria.")
    @Size(max = 80, message = "La modalidad no puede superar los 80 caracteres.")
    private String modalidad;

    @NotNull(message = "La cantidad despachada es obligatoria.")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0.")
    private Integer cantidadDespachada;

    @Size(max = 120, message = "El destino no puede superar los 120 caracteres.")
    private String destino;

    @Size(max = 80, message = "La guía de remisión no puede superar los 80 caracteres.")
    private String guiaRemision;

    @NotBlank(message = "La validación de calidad es obligatoria.")
    @Size(max = 120, message = "La validación no puede superar los 120 caracteres.")
    private String validacionCalidad;

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres.")
    private String observacion;

    @Size(max = 30, message = "El estado no puede superar los 30 caracteres.")
    private String estado = "REGISTRADO";

    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }
    public LocalDate getFechaDespacho() { return fechaDespacho; }
    public void setFechaDespacho(LocalDate fechaDespacho) { this.fechaDespacho = fechaDespacho; }
    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) { this.modalidad = modalidad; }
    public Integer getCantidadDespachada() { return cantidadDespachada; }
    public void setCantidadDespachada(Integer cantidadDespachada) { this.cantidadDespachada = cantidadDespachada; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getGuiaRemision() { return guiaRemision; }
    public void setGuiaRemision(String guiaRemision) { this.guiaRemision = guiaRemision; }
    public String getValidacionCalidad() { return validacionCalidad; }
    public void setValidacionCalidad(String validacionCalidad) { this.validacionCalidad = validacionCalidad; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
