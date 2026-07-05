package com.keraune.vlvblueberrysystem.api.controller;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.ApiResponse;
import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.AuthenticatedUserResponse;
import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.LoginRequest;
import com.keraune.vlvblueberrysystem.audit.AuditService;
import com.keraune.vlvblueberrysystem.security.LoginAttemptService;
import com.keraune.vlvblueberrysystem.security.LoginIdentifier;
import com.keraune.vlvblueberrysystem.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class ApiAuthController {
    private final AuthenticationManager authenticationManager; private final AccountService accountService; private final LoginAttemptService attempts; private final AuditService audit;
    public ApiAuthController(AuthenticationManager authenticationManager,AccountService accountService,LoginAttemptService attempts,AuditService audit){this.authenticationManager=authenticationManager;this.accountService=accountService;this.attempts=attempts;this.audit=audit;}
    public record CsrfResponse(String headerName,String parameterName,String token){}
    @GetMapping("/csrf") public ApiResponse<CsrfResponse> csrf(CsrfToken token){return ApiResponse.ok("Token CSRF disponible",new CsrfResponse(token.getHeaderName(),token.getParameterName(),token.getToken()));}
    @PostMapping("/login") public ApiResponse<AuthenticatedUserResponse> login(@Valid @RequestBody LoginRequest request,HttpServletRequest servletRequest){String key=LoginIdentifier.normalize(request.username())+"@"+servletRequest.getRemoteAddr();attempts.checkAllowed(key);try{Authentication auth=authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(),request.password()));SecurityContextHolder.getContext().setAuthentication(auth);servletRequest.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,SecurityContextHolder.getContext());attempts.succeeded(key);AuthenticatedUserResponse response=accountService.authenticatedUser(auth);audit.record("SESION","INICIAR_SESION","User",null,response.username(),"Se inició sesión en BlueberryTrace.");return ApiResponse.ok("Sesión iniciada",response);}catch(BadCredentialsException ex){attempts.failed(key);audit.record("SESION","INTENTO_FALLIDO","User",null,LoginIdentifier.normalize(request.username()),"Se registró un intento de inicio de sesión fallido.");throw ex;}}
    @PostMapping("/logout") public ApiResponse<Void> logout(HttpServletRequest request,HttpServletResponse response){Authentication auth=SecurityContextHolder.getContext().getAuthentication();if(auth!=null&&auth.isAuthenticated())audit.record("SESION","CERRAR_SESION","User",null,auth.getName(),"Se cerró la sesión.");new SecurityContextLogoutHandler().logout(request,response,auth);return ApiResponse.ok("Sesión cerrada",null);}
}
