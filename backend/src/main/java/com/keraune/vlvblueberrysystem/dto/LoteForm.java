package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record LoteForm(
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(max = 150) String descripcion,
        @Size(max = 120) String cultivo,
        @Size(max = 120) String variedad,
        @NotNull LocalDate fechaRegistro,
        @Size(max = 255) String observacion,
        @NotBlank @Size(max = 30) String estado
) {}
