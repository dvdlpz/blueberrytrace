package com.keraune.vlvblueberrysystem.config;

import com.keraune.vlvblueberrysystem.api.error.RestAccessDeniedHandler;
import com.keraune.vlvblueberrysystem.api.error.RestAuthenticationEntryPoint;
import com.keraune.vlvblueberrysystem.security.SecurityRoles;
import com.keraune.vlvblueberrysystem.security.UserSessionGuardFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Value("${blueberrytrace.security.cookie-secure:false}") private boolean cookieSecure;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository csrfTokenRepository,
                                            RestAuthenticationEntryPoint authenticationEntryPoint, RestAccessDeniedHandler accessDeniedHandler,
                                            UserSessionGuardFilter userSessionGuardFilter) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.migrateSession()))
                .addFilterAfter(userSessionGuardFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/health", "/actuator/health", "/api/v1/auth/csrf", "/api/v1/auth/login", "/api/v1/frontend/bootstrap").permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        .requestMatchers("/api/v1/usuarios", "/api/v1/usuarios/**", "/api/v1/roles", "/api/v1/roles/**").hasRole(SecurityRoles.ADMINISTRADOR)
                        .requestMatchers("/api/v1/**").hasAnyRole(SecurityRoles.allArray())
                        // The bundled React application is public; business data remains exclusively behind /api/v1/**.
                        .requestMatchers(HttpMethod.GET, "/", "/login", "/dashboard", "/lotes", "/camas", "/jabas", "/siembra", "/riegos", "/uniformizaciones", "/formalizaciones", "/procesos", "/clasificacion", "/recuperacion", "/pedidos", "/empaques", "/despacho", "/trazabilidad", "/reportes", "/usuarios", "/roles", "/lotes-trazables", "/mermas", "/auditoria", "/index.html", "/assets/**", "/favicon.ico").permitAll()
                        .anyRequest().denyAll())
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint).accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("BLUEBERRYTRACE-XSRF-TOKEN");
        repository.setHeaderName("X-BLUEBERRYTRACE-XSRF-TOKEN");
        repository.setCookieCustomizer(cookie -> cookie
                .path("/")
                .secure(cookieSecure)
                .sameSite(cookieSecure ? "Strict" : "Lax"));
        return repository;
    }
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)throws Exception{return configuration.getAuthenticationManager();}
}
