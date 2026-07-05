package com.keraune.vlvblueberrysystem.api.controller;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1")
public class ApiStatusController {
    public record ServiceStatusResponse(String name, String status, LocalDateTime timestamp) {}

    @GetMapping("/health")
    public ApiResponse<ServiceStatusResponse> health() {
        return ApiResponse.ok("Servicio disponible", new ServiceStatusResponse("BlueberryTrace", "UP", LocalDateTime.now()));
    }
}
