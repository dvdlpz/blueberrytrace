package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class LoteForm {

    @NotBlank(message = "El código del invernadero es obligatorio.")
    @Size(max = 30, message = "El código no puede superar los 30 caracteres.")
    private String codigo;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 150, message = "La descripción no puede superar los 150 caracteres.")
    private String descripcion;

    @NotBlank(message = "El cultivo es obligatorio.")
    @Size(max = 120, message = "El cultivo no puede superar los 120 caracteres.")
    private String cultivo;

    @NotBlank(message = "La variedad es obligatoria.")
    @Size(max = 120, message = "La variedad no puede superar los 120 caracteres.")
    private String variedad;

    @NotNull(message = "La fecha de registro es obligatoria.")
    private LocalDate fechaRegistro;

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres.")
    private String observacion;

    @NotBlank(message = "El estado es obligatorio.")
    @Size(max = 30, message = "El estado no puede superar los 30 caracteres.")
    private String estado = "ACTIVO";

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCultivo() { return cultivo; }
    public void setCultivo(String cultivo) { this.cultivo = cultivo; }
    public String getVariedad() { return variedad; }
    public void setVariedad(String variedad) { this.variedad = variedad; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
