package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.dto.AccountSettingsForm;
import com.keraune.vlvblueberrysystem.entity.User;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User obtenerUsuarioPorUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la cuenta autenticada."));
    }

    @Transactional(readOnly = true)
    public AccountSettingsForm construirFormulario(String username) {
        User user = obtenerUsuarioPorUsername(username);
        AccountSettingsForm form = new AccountSettingsForm();
        String[] partes = separarNombre(user.getNombreCompleto());
        form.setNombres(partes[0]);
        form.setApellidos(partes[1]);
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        return form;
    }

    @Transactional
    public void actualizarCuenta(String currentUsername, AccountSettingsForm form) {
        User user = obtenerUsuarioPorUsername(currentUsername);

        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es válida.");
        }

        userRepository.findByUsername(form.getUsername().trim()).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
            }
        });

        user.setNombreCompleto((form.getNombres().trim() + " " + form.getApellidos().trim()).trim());
        user.setUsername(form.getUsername().trim());
        user.setEmail(form.getEmail() == null ? null : form.getEmail().trim());
        userRepository.save(user);
    }

    private String[] separarNombre(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return new String[]{"", ""};
        }
        String limpio = nombreCompleto.trim();
        String[] tokens = limpio.split("\\s+", 2);
        if (tokens.length == 1) {
            return new String[]{tokens[0], ""};
        }
        return new String[]{tokens[0], tokens[1]};
    }
}
