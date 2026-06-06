package com.keraune.vlvblueberrysystem.controller;

import com.keraune.vlvblueberrysystem.dto.ClasificacionForm;
import com.keraune.vlvblueberrysystem.service.CamaService;
import com.keraune.vlvblueberrysystem.service.ClasificacionService;
import com.keraune.vlvblueberrysystem.service.LoteService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clasificacion")
public class ClasificacionController {

    private final ClasificacionService clasificacionService;
    private final LoteService loteService;
    private final CamaService camaService;

    public ClasificacionController(ClasificacionService clasificacionService, LoteService loteService, CamaService camaService) {
        this.clasificacionService = clasificacionService;
        this.loteService = loteService;
        this.camaService = camaService;
    }

    @GetMapping
    public String vistaClasificacion(Model model) {
        cargarModeloBase(model);
        return "clasificacion/index";
    }

    @PostMapping
    public String guardarClasificacion(
            @Valid @ModelAttribute("clasificacionForm") ClasificacionForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            cargarModeloBase(model);
            return "clasificacion/index";
        }

        try {
            clasificacionService.crearClasificacion(form, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Clasificación registrada correctamente.");
            return "redirect:/clasificacion";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("clasificacion.error", ex.getMessage());
            cargarModeloBase(model);
            return "clasificacion/index";
        }
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam(defaultValue = "VALIDADA") String estado,
                                RedirectAttributes redirectAttributes) {
        clasificacionService.cambiarEstado(id, estado);
        redirectAttributes.addFlashAttribute("successMessage", "Estado de clasificación actualizado.");
        return "redirect:/clasificacion";
    }

    private void cargarModeloBase(Model model) {
        if (!model.containsAttribute("clasificacionForm")) {
            model.addAttribute("clasificacionForm", clasificacionService.crearFormularioInicial());
        }
        model.addAttribute("lotes", loteService.listarTodos());
        model.addAttribute("camas", camaService.listarTodas());
        model.addAttribute("clasificaciones", clasificacionService.listarTodas());
        model.addAttribute("estadosPlanta", List.of("Óptimo", "Regular", "Observado", "Descartado"));
        model.addAttribute("tamanos", List.of("Grande", "Mediano", "Pequeño"));
        model.addAttribute("condiciones", List.of("Apta para despacho", "Requiere observación", "Requiere reproceso", "No apta"));
        model.addAttribute("estadosDisponibles", List.of("PENDIENTE", "VALIDADA", "OBSERVADA", "ANULADA"));
    }
}
