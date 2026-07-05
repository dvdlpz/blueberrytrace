package com.keraune.vlvblueberrysystem.api.error;

import com.keraune.vlvblueberrysystem.api.dto.ApiPayloads.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> validation(MethodArgumentNotValidException exception) {
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::messageOf)
                .collect(Collectors.joining("; "));
        return new ApiResponse<>(false, details.isBlank() ? "Datos inválidos." : details, null);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> badRequest(RuntimeException exception) {
        String message = exception.getMessage();
        return new ApiResponse<>(false, message == null || message.isBlank() ? "No se pudo procesar la solicitud." : message, null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> concurrentUpdate(OptimisticLockingFailureException exception) {
        return new ApiResponse<>(false, "El registro fue actualizado por otra persona. Actualiza la pantalla y vuelve a intentarlo.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> conflict(DataIntegrityViolationException exception) {
        return new ApiResponse<>(false, "No se pudo guardar el registro porque existe información duplicada o relacionada.", null);
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> unauthorized(RuntimeException exception) {
        return new ApiResponse<>(false, "Usuario, correo o contraseña incorrectos.", null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Error no controlado al procesar {} {}", request.getMethod(), request.getRequestURI(), exception);
        return new ApiResponse<>(false, "Ocurrió un error interno. Intenta nuevamente o contacta al administrador.", null);
    }

    private String messageOf(FieldError error) {
        return "Revisa el campo «" + fieldLabel(error.getField()) + "».";
    }

    private String fieldLabel(String field) {
        return switch (field) {
            case "username" -> "Usuario";
            case "email" -> "Correo empresarial";
            case "password", "currentPassword", "newPassword", "temporaryPassword" -> "Contraseña";
            case "nombreCompleto" -> "Nombre completo";
            case "cargo" -> "Cargo";
            case "rol", "rolId" -> "Rol";
            case "loteId", "loteFisicoId" -> "Invernadero";
            case "camaId", "camaInicialId" -> "Cama";
            case "loteTrazableId" -> "Lote trazable";
            case "clasificacionId" -> "Clasificación validada";
            case "cantidad", "cantidadRegistrada", "cantidadDespachada" -> "Cantidad de plantas";
            case "fechaSiembra", "fechaUniformizacion", "fechaFormalizacion", "fechaClasificacion", "fechaDespacho", "fechaIngreso", "fechaMerma" -> "Fecha de registro";
            case "codigo" -> "Código";
            case "variedad" -> "Variedad";
            case "procedencia" -> "Procedencia";
            case "destino" -> "Destino";
            case "motivo" -> "Motivo";
            case "observacion" -> "Observación";
            default -> "información requerida";
        };
    }
}
