package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CargaDespachoForm(
        @NotBlank @Size(max = 50) String codigo,
        @NotNull Long pedidoId,
        @NotNull LocalDate fechaCarga,
        @NotBlank @Size(max = 120) String vehiculo,
        @Size(max = 80) String guiaRemision,
        @Size(max = 255) String observacion
) {}
