package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class SiembraForm {

    @NotNull(message = "Debe seleccionar un invernadero.")
    private Long loteId;

    @NotNull(message = "Debe seleccionar una cama.")
    private Long camaId;

    @NotNull(message = "La fecha de siembra es obligatoria.")
    private LocalDate fechaSiembra;

    @NotNull(message = "La cantidad registrada es obligatoria.")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0.")
    private Integer cantidadRegistrada;

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres.")
    private String observacion;

    @Size(max = 30, message = "El estado no puede superar los 30 caracteres.")
    private String estado = "REGISTRADA";

    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }
    public Long getCamaId() { return camaId; }
    public void setCamaId(Long camaId) { this.camaId = camaId; }
    public LocalDate getFechaSiembra() { return fechaSiembra; }
    public void setFechaSiembra(LocalDate fechaSiembra) { this.fechaSiembra = fechaSiembra; }
    public Integer getCantidadRegistrada() { return cantidadRegistrada; }
    public void setCantidadRegistrada(Integer cantidadRegistrada) { this.cantidadRegistrada = cantidadRegistrada; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
