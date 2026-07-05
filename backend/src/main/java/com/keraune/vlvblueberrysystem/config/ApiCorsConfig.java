package com.keraune.vlvblueberrysystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ApiCorsConfig {
    @Value("${blueberrytrace.api.cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // React and the API share the same origin in the Pterodactyl JAR. CORS is
        // unnecessary in that topology and leaving this list empty intentionally
        // blocks cross-origin browser requests instead of accepting an unsafe wildcard.
        if (origins.isEmpty()) {
            return source;
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-Requested-With", "X-CSRF-TOKEN", "X-XSRF-TOKEN", "X-BLUEBERRYTRACE-XSRF-TOKEN"));
        configuration.setExposedHeaders(List.of("X-CSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        source.registerCorsConfiguration("/api/v1/**", configuration);
        return source;
    }
}
