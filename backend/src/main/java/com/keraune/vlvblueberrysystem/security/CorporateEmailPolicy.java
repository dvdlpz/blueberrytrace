package com.keraune.vlvblueberrysystem.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class CorporateEmailPolicy {
    private final List<String> allowedDomains;

    public CorporateEmailPolicy(@Value("${blueberrytrace.security.allowed-email-domains:}") String configuredDomains) {
        this.allowedDomains = Arrays.stream(configuredDomains.split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .toList();
    }

    public void validate(String email) {
        if (allowedDomains.isEmpty()) {
            return;
        }

        int separator = email.lastIndexOf('@');
        String domain = separator >= 0 ? email.substring(separator + 1).toLowerCase(Locale.ROOT) : "";
        if (!allowedDomains.contains(domain)) {
            throw new IllegalArgumentException("El correo debe pertenecer a un dominio corporativo autorizado.");
        }
    }
}
