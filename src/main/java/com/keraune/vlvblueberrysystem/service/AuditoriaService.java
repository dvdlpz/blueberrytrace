package com.keraune.vlvblueberrysystem.service;

import com.keraune.vlvblueberrysystem.entity.Auditoria;
import com.keraune.vlvblueberrysystem.entity.User;
import com.keraune.vlvblueberrysystem.repository.AuditoriaRepository;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UserRepository userRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, UserRepository userRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Auditoria> listarUltimasAcciones() {
        return auditoriaRepository.findTop50ByOrderByFechaHoraDesc();
    }

    @Transactional
    public void registrar(String modulo, String accion, String descripcion) {
        User usuario = obtenerUsuarioActual();
        Auditoria auditoria = new Auditoria(usuario, modulo, accion, descripcion);
        auditoriaRepository.save(auditoria);
    }

    private User obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username).orElse(null);
    }
}
