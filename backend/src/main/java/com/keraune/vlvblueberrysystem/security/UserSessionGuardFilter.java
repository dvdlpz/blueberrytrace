package com.keraune.vlvblueberrysystem.security;

import com.keraune.vlvblueberrysystem.entity.User;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Rejects stale, disabled or role-invalid sessions before a protected API request is handled. */
@Component
public class UserSessionGuardFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;

    public UserSessionGuardFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/")
                || request.getRequestURI().startsWith("/api/v1/auth/")
                || request.getRequestURI().equals("/api/v1/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            User user = userRepository.findByUsernameIgnoreCase(authentication.getName()).orElse(null);
            boolean valid = user != null && valid(user, authentication);
            if (!valid) {
                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }
                SecurityContextHolder.clearContext();
                write(response, HttpServletResponse.SC_UNAUTHORIZED, "Tu sesión fue actualizada o desactivada. Inicia sesión nuevamente.");
                return;
            }
            if (Boolean.TRUE.equals(user.getRequiereCambioPassword()) && !isPasswordChangeRoute(request)) {
                write(response, HttpServletResponse.SC_FORBIDDEN, "Debes actualizar tu contraseña temporal antes de continuar.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPasswordChangeRoute(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/api/v1/session/me") || uri.equals("/api/v1/session/me/password") || uri.equals("/api/v1/auth/logout");
    }

    private void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"success\":false,\"message\":\"" + message.replace("\"", "'") + "\",\"data\":null}");
    }

    private boolean valid(User user, Authentication authentication) {
        if (!Boolean.TRUE.equals(user.getEstado()) || user.getRole() == null || !Boolean.TRUE.equals(user.getRole().getEstado())
                || !SecurityRoles.isSupported(user.getRole().getNombre())) {
            return false;
        }
        Object principal = authentication.getPrincipal();
        return !(principal instanceof AuthenticatedUserPrincipal versioned)
                || versioned.getSessionVersion() == user.getSessionVersion();
    }
}
