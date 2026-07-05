package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountSettingsForm(
        @NotBlank @Size(max = 150) String nombreCompleto,
        @NotBlank @Email @Size(max = 120) String email,
        @Size(max = 90) String cargo,
        @Size(max = 30) String telefono,
        @Size(max = 30) String avatarColor
) {}
