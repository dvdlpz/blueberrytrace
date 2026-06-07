package com.keraune.vlvblueberrysystem.controller;

import com.keraune.vlvblueberrysystem.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/gestion")
    public String gestionAdministrador(Model model) {
        model.addAttribute("usuarios", userRepository.findAllByOrderByNombreCompletoAsc());
        return "admin/gestion";
    }
}
