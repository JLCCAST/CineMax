package com.cinemax.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;

// Llamadas a otro servicio que nadie atrapó: un rechazo (400, 404, 409) se responde igual;
// si el otro servicio no respondió o falló, 503 con un mensaje claro.
@RestControllerAdvice
@ConditionalOnClass(name = "feign.FeignException")
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class FeignExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(FeignExceptionHandler.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    public static final String SERVICIO_NO_DISPONIBLE =
            "Uno de nuestros servicios no responde en este momento. Intenta de nuevo en unos segundos.";

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiError> llamadaFallida(FeignException e, WebRequest request) {
        int status = e.status();
        if (status == 400 || status == 404 || status == 409) {
            return ApiExceptionHandler.responder(HttpStatus.valueOf(status), mensajeRemoto(e), request);
        }
        log.warn("Llamada a otro servicio fallida ({}): {}", status, e.getMessage());
        return ApiExceptionHandler.responder(HttpStatus.SERVICE_UNAVAILABLE, SERVICIO_NO_DISPONIBLE, request);
    }

    /** El campo "message" del cuerpo de error del otro servicio, si lo trae. */
    public static String mensajeRemoto(FeignException e) {
        try {
            JsonNode cuerpo = JSON.readTree(e.contentUTF8());
            if (cuerpo != null && cuerpo.hasNonNull("message")) {
                return cuerpo.get("message").asText();
            }
        } catch (Exception ignorado) {
            // Sin cuerpo legible: mensaje genérico.
        }
        return "No se pudo completar la operación.";
    }
}
