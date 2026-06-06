package com.keraune.vlvblueberrysystem.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/dashboard";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String register() {
        return "auth/register";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
