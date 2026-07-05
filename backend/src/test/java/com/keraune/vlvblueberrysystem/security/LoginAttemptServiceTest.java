package com.keraune.vlvblueberrysystem.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {
    @Test
    void bloqueaDespuesDelNumeroConfiguradoDeIntentosYLiberaTrasExito() {
        LoginAttemptService service = new LoginAttemptService(3, 60);
        service.failed("operario@vlv.agro.pe");
        service.failed("operario@vlv.agro.pe");
        assertDoesNotThrow(() -> service.checkAllowed("operario@vlv.agro.pe"));
        service.failed("operario@vlv.agro.pe");
        assertThrows(IllegalArgumentException.class, () -> service.checkAllowed("operario@vlv.agro.pe"));
        service.succeeded("operario@vlv.agro.pe");
        assertDoesNotThrow(() -> service.checkAllowed("operario@vlv.agro.pe"));
    }
}
