package com.cinemax.common.error;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

// Cuerpo de las respuestas de error de todos los servicios. El frontend muestra "message".
public record ApiError(Instant timestamp, int status, String error, String message, String path) {

    public static ApiError of(HttpStatusCode status, String message, String path) {
        HttpStatus conocido = HttpStatus.resolve(status.value());
        return new ApiError(Instant.now(), status.value(),
                conocido != null ? conocido.getReasonPhrase() : "Error", message, path);
    }
}
