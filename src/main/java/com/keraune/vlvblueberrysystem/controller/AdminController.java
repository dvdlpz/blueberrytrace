package com.keraune.vlvblueberrysystem.controller;

import com.keraune.vlvblueberrysystem.repository.UserRepository;
import com.keraune.vlvblueberrysystem.service.AuditoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditoriaService auditoriaService;

    public AdminController(UserRepository userRepository, AuditoriaService auditoriaService) {
        this.userRepository = userRepository;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/gestion")
    public String gestionAdministrador(Model model) {
        model.addAttribute("usuarios", userRepository.findAllByOrderByNombreCompletoAsc());
        return "admin/gestion";
    }

    @GetMapping("/auditoria")
    public String auditoria(Model model) {
        model.addAttribute("auditorias", auditoriaService.listarUltimasAcciones());
        return "admin/auditoria";
    }
}
