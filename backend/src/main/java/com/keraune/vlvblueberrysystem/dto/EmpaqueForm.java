package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record EmpaqueForm(
        @NotNull Long loteTrazableId,
        @NotNull Long clasificacionId,
        @NotNull Long pedidoDetalleId,
        @NotBlank @Size(max = 40) String tipo,
        @NotNull @Min(1) Integer capacidadPorUnidad,
        @NotNull @Min(1) Integer cantidadUnidades,
        @NotNull LocalDate fechaEmpaque,
        @Size(max = 255) String observacion
) {}
