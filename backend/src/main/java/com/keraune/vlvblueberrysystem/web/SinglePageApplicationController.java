package com.keraune.vlvblueberrysystem.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the React single-page application bundled inside the Pterodactyl JAR.
 *
 * <p>The frontend uses the browser history API. Direct navigation to an
 * operational route must therefore return {@code index.html}; React resolves
 * the screen after the client bundle is loaded. Static Vite assets are not
 * matched here and continue to be served by Spring Boot's resource handler.</p>
 */
@Controller
public class SinglePageApplicationController {
    private static final String FRONTEND_INDEX = "forward:/index.html";

    @GetMapping({
            "/",
            "/login",
            "/dashboard",
            "/lotes",
            "/camas",
            "/jabas",
            "/siembra",
            "/riegos",
            "/uniformizaciones",
            "/formalizaciones",
            "/procesos",
            "/clasificacion",
            "/recuperacion",
            "/pedidos",
            "/empaques",
            "/despacho",
            "/trazabilidad",
            "/reportes",
            "/usuarios",
            "/roles",
            "/lotes-trazables",
            "/mermas",
            "/auditoria"
    })
    public String forwardToFrontend() {
        return FRONTEND_INDEX;
    }
}
