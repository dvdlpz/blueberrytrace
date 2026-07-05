package com.keraune.vlvblueberrysystem.api.controller;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.AuthenticatedUserResponse;
import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.LoginRequest;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.security.LoginAttemptService;
import com.keraune.vlvblueberrysystem.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiAuthControllerTest {
    @Test
    void creaContextoDeSesionTrasLoginValido() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        AccountService accountService = mock(AccountService.class);
        Authentication authentication = mock(Authentication.class);
        when(manager.authenticate(any())).thenReturn(authentication);
        when(accountService.authenticatedUser(authentication)).thenReturn(new AuthenticatedUserResponse(
                "supervisor", "Supervisor", "supervisor@vlv.agro.pe", null, null,
                "emerald", null, "SUPERVISOR", false, List.of("ROLE_SUPERVISOR")
        ));
        ApiAuthController controller = new ApiAuthController(manager, accountService, new LoginAttemptService(5, 900), mock(AuditService.class));
        MockHttpServletRequest request = new MockHttpServletRequest();

        var response = controller.login(new LoginRequest("supervisor", "clave-segura"), request);

        assertEquals("Sesión iniciada", response.message());
        assertNotNull(request.getSession(false));
    }

    @Test
    void propagaCredencialesInvalidasParaQueElManejadorDevuelva401() {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("incorrecta"));
        ApiAuthController controller = new ApiAuthController(manager, mock(AccountService.class), new LoginAttemptService(5, 900), mock(AuditService.class));

        assertThrows(BadCredentialsException.class,
                () -> controller.login(new LoginRequest("supervisor", "incorrecta"), new MockHttpServletRequest()));
    }
}
