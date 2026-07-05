package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RecuperacionRiegoForm(
        @NotNull Long loteTrazableId,
        Long jabaId,
        @NotBlank @Size(max = 40) String etapaOrigen,
        @NotBlank @Size(max = 40) String etapaRetorno,
        @NotNull LocalDate fechaIngresoRiego,
        @NotNull @Min(1) Integer cantidadIngresada,
        @Size(max = 255) String observacion
) {}
