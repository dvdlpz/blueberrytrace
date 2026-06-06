package com.keraune.vlvblueberrysystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AccountSettingsForm {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres.")
    private String nombres;

    @NotBlank(message = "El apellido es obligatorio.")
    @Size(max = 80, message = "El apellido no puede superar los 80 caracteres.")
    private String apellidos;

    @NotBlank(message = "El usuario es obligatorio.")
    @Size(max = 50, message = "El usuario no puede superar los 50 caracteres.")
    private String username;

    @Email(message = "Ingresa un correo válido.")
    @Size(max = 120, message = "El correo no puede superar los 120 caracteres.")
    private String email;

    @NotBlank(message = "Debes confirmar tu contraseña actual para guardar cambios.")
    private String currentPassword;

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
}
