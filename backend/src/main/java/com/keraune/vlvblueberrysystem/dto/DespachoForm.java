package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record DespachoForm(
        @NotNull Long loteTrazableId,
        @NotNull Long clasificacionId,
        @NotNull Long loteId,
        @NotNull Long pedidoId,
        @NotNull Long pedidoDetalleId,
        @NotNull Long empaqueId,
        @NotNull @Min(1) Integer unidadesEmpaque,
        @NotNull LocalDate fechaDespacho,
        @Size(max = 120) String vehiculo,
        @Size(max = 80) String guiaRemision,
        @NotBlank @Size(max = 120) String validacionCalidad,
        @Size(max = 255) String observacion,
        @NotBlank @Size(max = 30) String estado
) {}
