package com.cinemax.common.error;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 401 sin sesión o con el token vencido, 403 sin permiso, con el formato de ApiError.
// Antes ambos casos respondían 403 y el frontend no podía distinguirlos.
@Component
public class RespuestasDeSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    public static final String SIN_SESION = "Inicia sesión para continuar. Si ya la tenías, venció.";
    public static final String SIN_PERMISO = "No tienes permiso para esta operación.";

    private final ObjectMapper objectMapper;

    public RespuestasDeSeguridad(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Sin sesión o con un token inválido o vencido: 401.
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escribir(response, HttpStatus.UNAUTHORIZED, SIN_SESION, request.getRequestURI());
    }

    // Con sesión, pero sin el permiso que pide la ruta: 403.
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escribir(response, HttpStatus.FORBIDDEN, SIN_PERMISO, request.getRequestURI());
    }

    private void escribir(HttpServletResponse response, HttpStatus status, String mensaje, String ruta)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(status, mensaje, ruta));
    }
}
