package com.keraune.vlvblueberrysystem.audit;

import com.keraune.vlvblueberrysystem.entity.AuditoriaOperacion;
import com.keraune.vlvblueberrysystem.entity.User;
import com.keraune.vlvblueberrysystem.repository.AuditoriaOperacionRepository;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Service
@Transactional
public class AuditService {
    private final AuditoriaOperacionRepository auditRepository;
    private final UserRepository userRepository;

    public AuditService(AuditoriaOperacionRepository auditRepository, UserRepository userRepository) {
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
    }

    public void record(String modulo, String accion, String entidadTipo, Long entidadId, String referencia, String descripcion) {
        record(modulo, accion, entidadTipo, entidadId, referencia, descripcion, null, null, null);
    }

    public void record(String modulo, String accion, String entidadTipo, Long entidadId, String referencia, String descripcion, String motivo) {
        record(modulo, accion, entidadTipo, entidadId, referencia, descripcion, motivo, null, null);
    }

    public void record(String modulo, String accion, String entidadTipo, Long entidadId, String referencia, String descripcion,
                       String motivo, String before, String after) {
        AuditoriaOperacion event = new AuditoriaOperacion();
        User user = currentUserOrNull();
        event.setUsuario(user);
        event.setRolNombre(user != null && user.getRole() != null ? user.getRole().getNombre() : null);
        event.setModulo(safe(modulo, "SISTEMA", 80));
        event.setAccion(safe(accion, "EVENTO", 80));
        event.setEntidadTipo(trim(entidadTipo, 80));
        event.setEntidadId(entidadId);
        event.setReferencia(trim(referencia, 160));
        event.setDescripcion(safe(descripcion, "Evento registrado", 255));
        event.setMotivo(trim(motivo, 255));
        event.setValoresAnteriores(trim(before, 16_000));
        event.setValoresPosteriores(trim(after, 16_000));
        RequestMeta meta = requestMeta();
        event.setIpOrigen(meta.ip());
        event.setAgenteUsuario(meta.userAgent());
        auditRepository.save(event);
    }

    private User currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return null;
        return userRepository.findByUsernameIgnoreCase(auth.getName()).orElse(null);
    }

    private RequestMeta requestMeta() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return new RequestMeta(null, null);
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        return new RequestMeta(trim(ip, 64), trim(request.getHeader("User-Agent"), 500));
    }

    private String safe(String value, String fallback, int max) {
        String candidate = trim(value, max);
        return candidate == null ? fallback : candidate;
    }

    private String trim(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String candidate = value.trim();
        return candidate.length() <= max ? candidate : candidate.substring(0, max);
    }

    private record RequestMeta(String ip, String userAgent) {}
}
