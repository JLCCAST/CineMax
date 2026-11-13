package com.cinemax.facturacion.client;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class Circuitos {

    private static final Map<String, String> NOMBRES = Map.of(
            "cartelera", "La cartelera",
            "confiteria", "La confitería",
            "sucursales", "El servicio de sedes y salas",
            "usuarios", "El servicio de usuarios");

    private final CircuitBreakerRegistry registro;

    public Circuitos() {
        this(Duration.ofSeconds(15));
    }

    Circuitos(Duration abiertoDurante) {
        this.registro = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(abiertoDurante)
                .permittedNumberOfCallsInHalfOpenState(2)
                .ignoreException(Circuitos::esRechazoDeNegocio)
                .build());
    }

    public <T> T llamar(String servicio, Supplier<T> llamada) {
        try {
            return registro.circuitBreaker(servicio).executeSupplier(llamada);
        } catch (CallNotPermittedException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    NOMBRES.getOrDefault(servicio, "Un servicio") + " no está disponible por ahora. "
                            + "Intenta de nuevo en unos segundos.");
        }
    }

    public void ejecutar(String servicio, Runnable llamada) {
        llamar(servicio, () -> {
            llamada.run();
            return null;
        });
    }

    private static boolean esRechazoDeNegocio(Throwable error) {
        if (error instanceof FeignException feign) {
            int status = feign.status();
            return status >= 400 && status < 500;
        }
        return error instanceof ResponseStatusException r && r.getStatusCode().is4xxClientError();
    }
}
