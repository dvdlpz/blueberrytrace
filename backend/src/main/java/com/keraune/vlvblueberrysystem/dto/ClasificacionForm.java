package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ClasificacionForm(
        @NotNull Long loteTrazableId,
        @NotNull Long loteId,
        @NotNull Long camaId,
        @NotNull Long jabaId,
        @NotNull LocalDate fechaClasificacion,
        @NotBlank @Size(max = 60) String estadoPlanta,
        @NotBlank @Size(max = 60) String tamano,
        @NotBlank @Size(max = 120) String condicion,
        @NotNull @Min(1) Integer cantidad,
        @Size(max = 255) String observacion,
        @NotBlank @Size(max = 30) String estado
) {}
