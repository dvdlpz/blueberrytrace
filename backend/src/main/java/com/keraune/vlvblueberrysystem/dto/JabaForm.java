package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.*;

public record JabaForm(
        @NotBlank @Size(max = 50) String codigo,
        @NotNull Long camaId,
        @NotNull @Min(1) Integer capacidadMacetas,
        @NotNull @Min(1) Integer ordenEnCama,
        @Size(max = 255) String observacion,
        @NotBlank @Size(max = 30) String estado
) {}
