package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class ClasificacionForm {

    @NotNull(message = "Debe seleccionar un invernadero.")
    private Long loteId;

    @NotNull(message = "Debe seleccionar una cama.")
    private Long camaId;

    @NotNull(message = "La fecha de clasificación es obligatoria.")
    private LocalDate fechaClasificacion;

    @NotBlank(message = "El estado de la planta es obligatorio.")
    @Size(max = 60, message = "El estado de la planta no puede superar los 60 caracteres.")
    private String estadoPlanta;

    @NotBlank(message = "El tamaño es obligatorio.")
    @Size(max = 60, message = "El tamaño no puede superar los 60 caracteres.")
    private String tamano;

    @NotBlank(message = "La condición es obligatoria.")
    @Size(max = 120, message = "La condición no puede superar los 120 caracteres.")
    private String condicion;

    @NotNull(message = "La cantidad clasificada es obligatoria.")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0.")
    private Integer cantidad;

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres.")
    private String observacion;

    @Size(max = 30, message = "El estado no puede superar los 30 caracteres.")
    private String estado = "PENDIENTE";

    public Long getLoteId() { return loteId; }
    public void setLoteId(Long loteId) { this.loteId = loteId; }
    public Long getCamaId() { return camaId; }
    public void setCamaId(Long camaId) { this.camaId = camaId; }
    public LocalDate getFechaClasificacion() { return fechaClasificacion; }
    public void setFechaClasificacion(LocalDate fechaClasificacion) { this.fechaClasificacion = fechaClasificacion; }
    public String getEstadoPlanta() { return estadoPlanta; }
    public void setEstadoPlanta(String estadoPlanta) { this.estadoPlanta = estadoPlanta; }
    public String getTamano() { return tamano; }
    public void setTamano(String tamano) { this.tamano = tamano; }
    public String getCondicion() { return condicion; }
    public void setCondicion(String condicion) { this.condicion = condicion; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
