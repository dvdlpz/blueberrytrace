package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record PedidoForm(
        @NotBlank @Size(max = 50) String codigo,
        @NotBlank @Size(max = 150) String cliente,
        @Size(max = 160) String destino,
        @NotNull LocalDate fechaCompromiso,
        @NotBlank @Size(max = 30) String estado,
        @Size(max = 255) String observacion,
        @NotEmpty List<@Valid PedidoDetalleForm> detalles
) {}
