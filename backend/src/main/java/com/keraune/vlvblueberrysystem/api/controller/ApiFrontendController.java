package com.keraune.vlvblueberrysystem.api.controller;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/frontend")
public class ApiFrontendController {
    @GetMapping("/bootstrap")
    public ApiResponse<FrontendBootstrapResponse> bootstrap() {
        PaletteResponse palette = new PaletteResponse("#082817", "#227848", "#1c97d8", "#7c3aed", "#84cc16", "#f59e0b", "#ffffff", "#eef3ef");
        FrontendBootstrapResponse response = new FrontendBootstrapResponse(
                "BlueberryTrace",
                "v1",
                "Aplicación web de trazabilidad",
                List.of("Aplicación web", "Trazabilidad operativa"),
                ApiModuleMetadata.endpoints(),
                ApiModuleMetadata.modules(),
                palette
        );
        return ApiResponse.ok("La aplicación está lista para usarse", response);
    }
}
