package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record UniformizacionForm(
        @NotNull Long loteTrazableId,
        @NotNull Long loteId,
        @NotNull Long camaId,
        @NotNull Long jabaOrigenId,
        @NotNull Long jabaDestinoId,
        @NotNull LocalDate fechaUniformizacion,
        @NotBlank @Size(max = 120) String criterio,
        @NotNull @Min(1) Integer cantidadInicial,
        @NotNull @Min(1) Integer cantidadUniformizada,
        @NotBlank @Size(max = 40) String origenOperativo,
        @NotNull @Min(0) Integer cantidadRecuperacion,
        @NotNull Boolean malezasRetiradas,
        @Size(max = 255) String observacion,
        @NotBlank @Size(max = 30) String estado
) {}
