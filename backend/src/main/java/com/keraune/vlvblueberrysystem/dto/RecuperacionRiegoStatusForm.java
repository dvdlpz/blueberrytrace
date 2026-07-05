package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.*;

public record RecuperacionRiegoStatusForm(
        @NotNull @Min(0) Integer cantidadRecuperada,
        @NotNull @Min(0) Integer cantidadDescartada,
        @Size(max = 120) String motivoDescarte,
        @Size(max = 255) String observacion
) {}
