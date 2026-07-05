package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CamaForm(
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(max = 150) String descripcion,
        @NotNull @Min(1) Integer capacidadReferencial,
        @NotBlank @Size(max = 30) String estado,
        @NotNull Long loteId
) {}
