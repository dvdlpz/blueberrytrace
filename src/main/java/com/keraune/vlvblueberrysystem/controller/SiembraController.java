package com.keraune.vlvblueberrysystem.controller;

import com.keraune.vlvblueberrysystem.dto.SiembraForm;
import com.keraune.vlvblueberrysystem.service.CamaService;
import com.keraune.vlvblueberrysystem.service.LoteService;
import com.keraune.vlvblueberrysystem.service.SiembraService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/siembra")
public class SiembraController {

    private final SiembraService siembraService;
    private final LoteService loteService;
    private final CamaService camaService;

    public SiembraController(SiembraService siembraService, LoteService loteService, CamaService camaService) {
        this.siembraService = siembraService;
        this.loteService = loteService;
        this.camaService = camaService;
    }

    @GetMapping
    public String vistaSiembra(Model model) {
        cargarModeloBase(model);
        return "siembra/index";
    }

    @PostMapping
    public String guardarSiembra(
            @Valid @ModelAttribute("siembraForm") SiembraForm siembraForm,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            cargarModeloBase(model);
            return "siembra/index";
        }

        try {
            siembraService.crearSiembra(siembraForm, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Siembra registrada correctamente.");
            return "redirect:/siembra";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("siembra.error", ex.getMessage());
            cargarModeloBase(model);
            return "siembra/index";
        }
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        siembraService.cambiarEstado(id);
        redirectAttributes.addFlashAttribute("successMessage", "Estado del registro de siembra actualizado.");
        return "redirect:/siembra";
    }

    private void cargarModeloBase(Model model) {
        if (!model.containsAttribute("siembraForm")) {
            model.addAttribute("siembraForm", siembraService.crearFormularioInicial());
        }
        model.addAttribute("lotes", loteService.listarTodos());
        model.addAttribute("camas", camaService.listarTodas());
        model.addAttribute("siembras", siembraService.listarTodas());
        model.addAttribute("estadosDisponibles", List.of("REGISTRADA", "ANULADA"));
    }
}
