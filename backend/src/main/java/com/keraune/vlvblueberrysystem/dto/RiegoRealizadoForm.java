package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record RiegoRealizadoForm(
        @NotNull LocalDate fechaEjecucion,
        @NotNull LocalTime horaEjecucion,
        @Size(max = 255) String observacion
) {}
