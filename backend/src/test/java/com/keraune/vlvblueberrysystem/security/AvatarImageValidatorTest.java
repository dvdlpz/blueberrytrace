package com.keraune.vlvblueberrysystem.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AvatarImageValidatorTest {
    private final AvatarImageValidator validator = new AvatarImageValidator(32);

    @Test
    void aceptaPngConFirmaValidaYNormalizaLaCarga() {
        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        String input = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngHeader);

        assertEquals(input, validator.validateAndNormalize(input));
        assertNull(validator.validateAndNormalize("  "));
    }

    @Test
    void rechazaFirmaQueNoCoincideConElMimeDeclarado() {
        String input = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(new byte[]{0x00, 0x01, 0x02});

        assertThrows(IllegalArgumentException.class, () -> validator.validateAndNormalize(input));
    }

    @Test
    void rechazaCargaQueSuperaElLimiteRealEnBytes() {
        AvatarImageValidator shortLimit = new AvatarImageValidator(8);
        byte[] pngHeaderAndExtra = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        String input = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngHeaderAndExtra);

        assertThrows(IllegalArgumentException.class, () -> shortLimit.validateAndNormalize(input));
    }
}
