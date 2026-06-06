package com.keraune.vlvblueberrysystem.controller;

import com.keraune.vlvblueberrysystem.service.TrazabilidadQueryService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final TrazabilidadQueryService trazabilidadQueryService;

    public ReporteController(TrazabilidadQueryService trazabilidadQueryService) {
        this.trazabilidadQueryService = trazabilidadQueryService;
    }

    @GetMapping
    public String vistaReportes(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String variedad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model) {

        model.addAttribute("codigo", codigo == null ? "" : codigo.trim());
        model.addAttribute("variedad", variedad == null ? "" : variedad.trim());
        model.addAttribute("fecha", fecha);
        model.addAttribute("resultados", trazabilidadQueryService.buscar(codigo, variedad, fecha));
        return "reportes/index";
    }
}
