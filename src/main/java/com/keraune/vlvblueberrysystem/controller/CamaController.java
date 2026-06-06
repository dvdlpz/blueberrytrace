package com.keraune.vlvblueberrysystem.controller;

import com.keraune.vlvblueberrysystem.dto.CamaForm;
import com.keraune.vlvblueberrysystem.service.CamaService;
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
@RequestMapping("/camas")
public class CamaController {

    private final CamaService camaService;
    private final LoteService loteService;

    public CamaController(CamaService camaService, LoteService loteService) {
        this.camaService = camaService;
        this.loteService = loteService;
    }

    @GetMapping
    public String listarCamas(
            @RequestParam(name = "edit", required = false) Long editId,
            Model model
    ) {
        if (!model.containsAttribute("camaForm")) {
            model.addAttribute("camaForm", new CamaForm());
        }

        model.addAttribute("camas", camaService.listarTodas());
        model.addAttribute("lotes", loteService.listarTodos());
        model.addAttribute("estadosDisponibles", List.of("ACTIVA", "INACTIVA", "MANTENIMIENTO"));

        if (editId != null) {
            if (!model.containsAttribute("camaEditForm")) {
                model.addAttribute("camaEditForm", camaService.construirFormularioDesdeEntidad(editId));
            }
            model.addAttribute("editId", editId);
        }

        return "camas/lista";
    }

    @PostMapping
    public String guardarCama(
            @Valid @ModelAttribute("camaForm") CamaForm camaForm,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            cargarModeloBase(model);
            return "camas/lista";
        }

        try {
            camaService.crearCama(camaForm, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Cama registrada correctamente.");
            return "redirect:/camas";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("codigo", "error.camaForm", ex.getMessage());
            cargarModeloBase(model);
            return "camas/lista";
        }
    }

    @PostMapping("/{id}/editar")
    public String actualizarCama(
            @PathVariable Long id,
            @Valid @ModelAttribute("camaEditForm") CamaForm camaEditForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            cargarModeloBase(model);
            model.addAttribute("editId", id);
            return "camas/lista";
        }

        try {
            camaService.actualizarCama(id, camaEditForm);
            redirectAttributes.addFlashAttribute("successMessage", "Cama actualizada correctamente.");
            return "redirect:/camas";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("codigo", "error.camaEditForm", ex.getMessage());
            cargarModeloBase(model);
            model.addAttribute("editId", id);
            return "camas/lista";
        }
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        camaService.cambiarEstado(id);
        redirectAttributes.addFlashAttribute("successMessage", "Estado de la cama actualizado correctamente.");
        return "redirect:/camas";
    }

    private void cargarModeloBase(Model model) {
        if (!model.containsAttribute("camaForm")) {
            model.addAttribute("camaForm", new CamaForm());
        }
        model.addAttribute("camas", camaService.listarTodas());
        model.addAttribute("lotes", loteService.listarTodos());
        model.addAttribute("estadosDisponibles", List.of("ACTIVA", "INACTIVA", "MANTENIMIENTO"));
    }
}
