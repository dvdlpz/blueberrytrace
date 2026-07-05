package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record SiembraForm(
        @NotNull Long loteTrazableId,
        @NotNull Long loteId,
        @NotNull Long camaId,
        @NotNull Long jabaId,
        @NotNull LocalDate fechaSiembra,
        @NotNull @Min(1) Integer cantidadRegistrada,
        @Size(max = 255) String observacion,
        @NotBlank @Size(max = 30) String estado
) {}
