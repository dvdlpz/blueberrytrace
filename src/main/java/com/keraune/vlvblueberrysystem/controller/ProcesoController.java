package com.keraune.vlvblueberrysystem.controller;

import com.keraune.vlvblueberrysystem.dto.FormalizacionForm;
import com.keraune.vlvblueberrysystem.dto.UniformizacionForm;
import com.keraune.vlvblueberrysystem.service.CamaService;
import com.keraune.vlvblueberrysystem.service.LoteService;
import com.keraune.vlvblueberrysystem.service.ProcesoOperativoService;
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
@RequestMapping("/procesos")
public class ProcesoController {

    private final ProcesoOperativoService procesoOperativoService;
    private final LoteService loteService;
    private final CamaService camaService;

    public ProcesoController(ProcesoOperativoService procesoOperativoService, LoteService loteService, CamaService camaService) {
        this.procesoOperativoService = procesoOperativoService;
        this.loteService = loteService;
        this.camaService = camaService;
    }

    @GetMapping
    public String vistaProcesos(Model model) {
        cargarModeloBase(model);
        return "procesos/index";
    }

    @PostMapping("/uniformizacion")
    public String guardarUniformizacion(
            @Valid @ModelAttribute("uniformizacionForm") UniformizacionForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            cargarModeloBase(model);
            return "procesos/index";
        }

        try {
            procesoOperativoService.crearUniformizacion(form, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Uniformización registrada correctamente.");
            return "redirect:/procesos";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("uniformizacion.error", ex.getMessage());
            cargarModeloBase(model);
            return "procesos/index";
        }
    }

    @PostMapping("/formalizacion")
    public String guardarFormalizacion(
            @Valid @ModelAttribute("formalizacionForm") FormalizacionForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            cargarModeloBase(model);
            return "procesos/index";
        }

        try {
            procesoOperativoService.crearFormalizacion(form, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Formalización registrada correctamente.");
            return "redirect:/procesos";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("formalizacion.error", ex.getMessage());
            cargarModeloBase(model);
            return "procesos/index";
        }
    }

    @PostMapping("/uniformizacion/{id}/estado")
    public String cambiarEstadoUniformizacion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        procesoOperativoService.cambiarEstadoUniformizacion(id);
        redirectAttributes.addFlashAttribute("successMessage", "Estado de uniformización actualizado.");
        return "redirect:/procesos";
    }

    @PostMapping("/formalizacion/{id}/estado")
    public String cambiarEstadoFormalizacion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        procesoOperativoService.cambiarEstadoFormalizacion(id);
        redirectAttributes.addFlashAttribute("successMessage", "Estado de formalización actualizado.");
        return "redirect:/procesos";
    }

    private void cargarModeloBase(Model model) {
        if (!model.containsAttribute("uniformizacionForm")) {
            model.addAttribute("uniformizacionForm", procesoOperativoService.crearFormularioUniformizacionInicial());
        }
        if (!model.containsAttribute("formalizacionForm")) {
            model.addAttribute("formalizacionForm", procesoOperativoService.crearFormularioFormalizacionInicial());
        }
        model.addAttribute("lotes", loteService.listarTodos());
        model.addAttribute("camas", camaService.listarTodas());
        model.addAttribute("uniformizaciones", procesoOperativoService.listarUniformizaciones());
        model.addAttribute("formalizaciones", procesoOperativoService.listarFormalizaciones());
        model.addAttribute("estadosDisponibles", List.of("REGISTRADA", "ANULADA"));
    }
}
