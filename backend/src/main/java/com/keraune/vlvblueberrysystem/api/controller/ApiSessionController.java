package com.keraune.vlvblueberrysystem.api.controller;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.*;
import com.keraune.vlvblueberrysystem.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/session")
public class ApiSessionController {
    private final AccountService accountService;

    public ApiSessionController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public ApiResponse<AuthenticatedUserResponse> me() {
        return ApiResponse.ok("Sesión activa", accountService.currentUserResponse());
    }

    @PutMapping("/me")
    public ApiResponse<AuthenticatedUserResponse> updateProfile(@Valid @RequestBody ProfileUpdatePayload payload) {
        return ApiResponse.ok("Perfil actualizado", accountService.updateProfile(payload));
    }

    @PatchMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangePayload payload) {
        accountService.changePassword(payload);
        return ApiResponse.ok("Contraseña actualizada", null);
    }
}
