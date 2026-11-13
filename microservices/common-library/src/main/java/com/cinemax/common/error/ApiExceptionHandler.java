package com.cinemax.common.error;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// Errores con el mismo formato en todos los servicios. El código nuevo usa ResponseStatusException;
// el anterior lanza RuntimeException con mensaje ("no encontrad..." es 404 y el resto, 400).
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> estadoPropio(ResponseStatusException e, WebRequest request) {
        return responder(e.getStatusCode(), e.getReason(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException e, WebRequest request) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : error.getField() + " no es válido")
                .sorted()
                .collect(Collectors.joining(". "));
        return responder(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> peticionMalFormada(Exception e, WebRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "La petición no tiene el formato esperado.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> rutaInexistente(NoResourceFoundException e, WebRequest request) {
        return responder(HttpStatus.NOT_FOUND, "La ruta no existe.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> metodoNoPermitido(HttpRequestMethodNotSupportedException e, WebRequest request) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "La ruta no admite " + e.getMethod() + ".", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> formatoNoAdmitido(HttpMediaTypeNotSupportedException e, WebRequest request) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El formato del cuerpo no es el esperado.", request);
    }

    // @PreAuthorize sin permiso. Sin sesión es 401 (que inicie sesión); con sesión, 403.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accesoDenegado(AccessDeniedException e, WebRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean anonimo = auth == null || auth instanceof AnonymousAuthenticationToken;
        return anonimo
                ? responder(HttpStatus.UNAUTHORIZED, RespuestasDeSeguridad.SIN_SESION, request)
                : responder(HttpStatus.FORBIDDEN, RespuestasDeSeguridad.SIN_PERMISO, request);
    }

    // Por ejemplo, BadCredentialsException en el login: 401 con el mensaje del servicio.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> autenticacion(AuthenticationException e, WebRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
    }

    // Sucursales la usa para "Sede no encontrada"; un orElseThrow() sin mensaje también.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> noEncontrado(NoSuchElementException e, WebRequest request) {
        String mensaje = e.getMessage() == null || "No value present".equals(e.getMessage())
                ? "No se encontró lo que buscas." : e.getMessage();
        return responder(HttpStatus.NOT_FOUND, mensaje, request);
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiError> reglaDeNegocio(RuntimeException e, WebRequest request) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> otros(RuntimeException e, WebRequest request) {
        // Solo un RuntimeException "a secas" es un error de negocio con mensaje para el
        // usuario. Un NullPointerException u otro fallo es un error interno.
        if (e.getClass() != RuntimeException.class || e.getMessage() == null) {
            log.error("Error interno en {}", ruta(request), e);
            return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor.", request);
        }
        HttpStatus status = e.getMessage().toLowerCase().contains("no encontrad")
                ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return responder(status, e.getMessage(), request);
    }

    static ResponseEntity<ApiError> responder(HttpStatusCode status, String mensaje, WebRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(status, mensaje, ruta(request)));
    }

    private static String ruta(WebRequest request) {
        return request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : null;
    }
}
