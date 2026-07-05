package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record FormalizacionForm(
        @NotNull Long loteTrazableId,
        @NotNull Long loteId,
        @NotNull Long camaId,
        @NotNull LocalDate fechaFormalizacion,
        @NotBlank @Size(max = 180) String detalle,
        @NotNull @Min(1) Integer cantidadBandejas,
        @NotEmpty List<@NotNull Long> jabaIds,
        @NotNull @Min(1) Integer cantidadPlantas,
        @NotBlank @Size(max = 40) String ordenamientoJabas,
        @Size(max = 255) String observacion,
        @NotBlank @Size(max = 30) String estado
) {}
