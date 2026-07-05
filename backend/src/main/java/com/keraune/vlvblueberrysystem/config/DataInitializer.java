package com.keraune.vlvblueberrysystem.config;

import com.keraune.vlvblueberrysystem.entity.Role;
import com.keraune.vlvblueberrysystem.entity.User;
import com.keraune.vlvblueberrysystem.repository.RoleRepository;
import com.keraune.vlvblueberrysystem.repository.UserRepository;
import com.keraune.vlvblueberrysystem.security.SecurityRoles;
import com.keraune.vlvblueberrysystem.service.RolePermissionService;
import com.keraune.vlvblueberrysystem.security.CorporateEmailPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedSecurityData(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            CorporateEmailPolicy corporateEmailPolicy,
            RolePermissionService rolePermissionService,
            @Value("${blueberrytrace.bootstrap-admin.enabled:false}") boolean bootstrapEnabled,
            @Value("${blueberrytrace.bootstrap-admin.username:}") String bootstrapUsername,
            @Value("${blueberrytrace.bootstrap-admin.password:}") String bootstrapPassword,
            @Value("${blueberrytrace.bootstrap-admin.email:}") String bootstrapEmail,
            @Value("${blueberrytrace.bootstrap-admin.full-name:}") String bootstrapName,
            @Value("${blueberrytrace.bootstrap-admin.position:}") String bootstrapPosition
    ) {
        return args -> {
            Map<String, String> roles = new LinkedHashMap<>();
            roles.put(SecurityRoles.ADMINISTRADOR, "Acceso administrativo integral al sistema");
            roles.put(SecurityRoles.SUPERVISOR, "Supervisa y administra la operación de vivero");
            roles.put(SecurityRoles.OPERARIO, "Registra siembra y procesos operativos autorizados");
            roles.put(SecurityRoles.CONTROL_CALIDAD, "Valida clasificación, calidad y despacho");
            roles.put(SecurityRoles.CONSULTA, "Consulta información y reportes sin modificar registros");

            for (Map.Entry<String, String> entry : roles.entrySet()) {
                Role role = roleRepository.findByNombreIgnoreCase(entry.getKey()).orElseGet(() -> {
                    Role created = new Role();
                    created.setNombre(entry.getKey());
                    created.setDescripcion(entry.getValue());
                    created.setColor(defaultRoleColor(entry.getKey()));
                    created.setEstado(true);
                    return created;
                });
                if (role.getDescripcion() == null || role.getDescripcion().isBlank()) {
                    role.setDescripcion(entry.getValue());
                }
                if (role.getColor() == null || role.getColor().isBlank()) {
                    role.setColor(defaultRoleColor(entry.getKey()));
                }
                roleRepository.save(role);
                rolePermissionService.ensureDefaults(role);
            }

            if (!bootstrapEnabled) {
                return;
            }
            if (bootstrapUsername.isBlank() || bootstrapEmail.isBlank() || bootstrapName.isBlank() || bootstrapPosition.isBlank() || bootstrapPassword.length() < 12) {
                throw new IllegalStateException("El bootstrap administrativo requiere usuario, nombre completo, cargo, correo y contraseña de al menos 12 caracteres.");
            }
            if (containsExampleValue(bootstrapUsername, bootstrapEmail, bootstrapName, bootstrapPosition, bootstrapPassword)) {
                throw new IllegalStateException("El bootstrap administrativo contiene valores de ejemplo. Reemplázalos por valores privados antes de habilitarlo.");
            }
            corporateEmailPolicy.validate(bootstrapEmail.trim());
            if (userRepository.existsByUsernameIgnoreCase(bootstrapUsername) || userRepository.existsByEmailIgnoreCase(bootstrapEmail)) {
                log.warn("No se creó la cuenta administrativa inicial porque el usuario o correo ya existe.");
                return;
            }

            Role adminRole = roleRepository.findByNombreIgnoreCase(SecurityRoles.ADMINISTRADOR).orElseThrow();
            User administrator = new User();
            administrator.setUsername(bootstrapUsername.trim());
            administrator.setNombreCompleto(bootstrapName.trim());
            administrator.setEmail(bootstrapEmail.trim().toLowerCase());
            administrator.setCargo(bootstrapPosition.trim());
            administrator.setAvatarColor("emerald");
            administrator.setEstado(true);
            administrator.setRole(adminRole);
            administrator.setPassword(passwordEncoder.encode(bootstrapPassword));
            userRepository.save(administrator);
            log.info("Cuenta administrativa inicial creada de forma controlada.");
        };
    }

    private String defaultRoleColor(String role) {
        return switch (role) {
            case SecurityRoles.ADMINISTRADOR -> "#6D28D9";
            case SecurityRoles.SUPERVISOR -> "#0F766E";
            case SecurityRoles.OPERARIO -> "#C26A08";
            case SecurityRoles.CONTROL_CALIDAD -> "#0E7490";
            default -> "#475569";
        };
    }

    private boolean containsExampleValue(String... values) {
        for (String value : values) {
            String normalized = value == null ? "" : value.trim().toUpperCase();
            if (normalized.contains("CAMBIAR_") || normalized.contains("EJEMPLO") || normalized.contains("EXAMPLE")) {
                return true;
            }
        }
        return false;
    }
}
