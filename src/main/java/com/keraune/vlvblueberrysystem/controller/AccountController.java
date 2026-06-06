package com.keraune.vlvblueberrysystem.controller;

import com.keraune.vlvblueberrysystem.dto.AccountSettingsForm;
import com.keraune.vlvblueberrysystem.service.AccountService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cuenta")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/ajustes")
    public String ajustesCuenta(Principal principal, Model model) {
        if (!model.containsAttribute("accountSettingsForm")) {
            model.addAttribute("accountSettingsForm", accountService.construirFormulario(principal.getName()));
        }
        model.addAttribute("activePage", "cuenta");
        return "cuenta/ajustes";
    }

    @PostMapping("/ajustes")
    public String actualizarCuenta(
            @Valid @ModelAttribute("accountSettingsForm") AccountSettingsForm form,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "cuenta");
            return "cuenta/ajustes";
        }

        try {
            accountService.actualizarCuenta(principal.getName(), form);
            redirectAttributes.addFlashAttribute("successMessage", "Los datos de tu cuenta se actualizaron correctamente.");
            return "redirect:/cuenta/ajustes";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("currentPassword", "error.accountSettingsForm", ex.getMessage());
            model.addAttribute("activePage", "cuenta");
            return "cuenta/ajustes";
        }
    }
}
