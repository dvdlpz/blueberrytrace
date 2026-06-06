package com.keraune.vlvblueberrysystem.controller;

import com.keraune.vlvblueberrysystem.dto.DespachoForm;
import com.keraune.vlvblueberrysystem.service.DespachoService;
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
@RequestMapping("/despacho")
public class DespachoController {

    private final DespachoService despachoService;
    private final LoteService loteService;

    public DespachoController(DespachoService despachoService, LoteService loteService) {
        this.despachoService = despachoService;
        this.loteService = loteService;
    }

    @GetMapping
    public String vistaDespacho(Model model) {
        cargarModeloBase(model);
        return "despacho/index";
    }

    @PostMapping
    public String guardarDespacho(
            @Valid @ModelAttribute("despachoForm") DespachoForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            cargarModeloBase(model);
            return "despacho/index";
        }

        try {
            despachoService.crearDespacho(form, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Despacho registrado correctamente.");
            return "redirect:/despacho";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("despacho.error", ex.getMessage());
            cargarModeloBase(model);
            return "despacho/index";
        }
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam(defaultValue = "CERRADO") String estado,
                                RedirectAttributes redirectAttributes) {
        despachoService.cambiarEstado(id, estado);
        redirectAttributes.addFlashAttribute("successMessage", "Estado de despacho actualizado.");
        return "redirect:/despacho";
    }

    private void cargarModeloBase(Model model) {
        if (!model.containsAttribute("despachoForm")) {
            model.addAttribute("despachoForm", despachoService.crearFormularioInicial());
        }
        model.addAttribute("lotes", loteService.listarTodos());
        model.addAttribute("despachos", despachoService.listarTodos());
        model.addAttribute("modalidades", List.of("JABAS", "BINS_MADERA"));
        model.addAttribute("validacionesCalidad", List.of("APROBADO", "OBSERVADO", "RECHAZADO"));
        model.addAttribute("estadosDisponibles", List.of("REGISTRADO", "CERRADO", "OBSERVADO", "ANULADO"));
    }
}
