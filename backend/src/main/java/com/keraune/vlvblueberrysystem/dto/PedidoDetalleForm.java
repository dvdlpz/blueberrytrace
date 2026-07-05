package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.*;

public record PedidoDetalleForm(
        @NotBlank @Size(max = 120) String variedad,
        @NotNull @Min(1) Integer cantidadSolicitada,
        @Size(max = 255) String observacion
) {}
