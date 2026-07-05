package com.keraune.vlvblueberrysystem.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AvatarImageValidator {
    private static final Pattern DATA_URI = Pattern.compile("^data:(image/(?:png|jpeg|webp));base64,([A-Za-z0-9+/]+={0,2})$");

    private final int maxBytes;

    public AvatarImageValidator(@Value("${blueberrytrace.profile-image.max-bytes:1000000}") int maxBytes) {
        if (maxBytes < 1) {
            throw new IllegalArgumentException("El límite de imagen de perfil debe ser mayor a cero.");
        }
        this.maxBytes = maxBytes;
    }

    /**
     * The application persists an already validated data URI. It never accepts or resolves a server-side file name,
     * which prevents path traversal by design.
     */
    public String validateAndNormalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String clean = value.trim();
        int maxTransportLength = (int) Math.ceil(maxBytes * 4.0 / 3.0) + 256;
        if (clean.length() > maxTransportLength) {
            throw new IllegalArgumentException("La imagen de perfil supera el tamaño permitido.");
        }

        Matcher matcher = DATA_URI.matcher(clean);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("La imagen de perfil debe ser PNG, JPG o WEBP codificada de forma válida.");
        }

        String mimeType = matcher.group(1).toLowerCase(Locale.ROOT);
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(matcher.group(2));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("La imagen de perfil no contiene datos válidos.");
        }

        if (bytes.length == 0 || bytes.length > maxBytes) {
            throw new IllegalArgumentException("La imagen de perfil supera el tamaño permitido.");
        }

        if (!matchesDeclaredFormat(mimeType, bytes)) {
            throw new IllegalArgumentException("El contenido de la imagen no coincide con el formato declarado.");
        }

        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private boolean matchesDeclaredFormat(String mimeType, byte[] bytes) {
        return switch (mimeType) {
            case "image/png" -> matchesPng(bytes);
            case "image/jpeg" -> matchesJpeg(bytes);
            case "image/webp" -> matchesWebp(bytes);
            default -> false;
        };
    }

    private boolean matchesPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private boolean matchesJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean matchesWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P';
    }
}
