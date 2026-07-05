package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record RiegoProgramadoForm(
        @NotNull Long loteTrazableId,
        @NotNull Long camaId,
        Long jabaId,
        @NotNull LocalDate fechaProgramada,
        @NotNull LocalTime horaProgramada,
        @NotBlank @Size(max = 40) String etapaAplicacion,
        @Size(max = 255) String observacion
) {}
