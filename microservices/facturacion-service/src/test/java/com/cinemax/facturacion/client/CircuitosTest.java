package com.cinemax.facturacion.client;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitosTest {

    private final Circuitos circuitos = new Circuitos(Duration.ofMillis(300));
    private final AtomicInteger llamadas = new AtomicInteger();

    @Test
    void si_el_servicio_falla_varias_veces_se_corta_sin_llamarlo() {
        for (int i = 0; i < 5; i++) {
            assertThrows(FeignException.class, () -> circuitos.llamar("confiteria", this::caido));
        }

        ResponseStatusException cortado = assertThrows(ResponseStatusException.class,
                () -> circuitos.llamar("confiteria", this::caido));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, cortado.getStatusCode());
        assertTrue(cortado.getReason().startsWith("La confitería no está disponible"));
        assertEquals(5, llamadas.get(), "con el circuito abierto no se llama al servicio");
    }

    @Test
    void cuando_el_servicio_vuelve_el_circuito_se_cierra() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            assertThrows(FeignException.class, () -> circuitos.llamar("cartelera", this::caido));
        }
        Thread.sleep(400);

        assertEquals("ok", circuitos.llamar("cartelera", () -> "ok"));
        assertEquals("ok", circuitos.llamar("cartelera", () -> "ok"));
        assertEquals("ok", circuitos.llamar("cartelera", () -> "ok"));
    }

    @Test
    void los_rechazos_de_negocio_no_abren_el_circuito() {
        for (int i = 0; i < 10; i++) {
            assertThrows(FeignException.class, () -> circuitos.llamar("confiteria", () -> rechazo(409)));
            assertThrows(FeignException.class, () -> circuitos.llamar("confiteria", () -> rechazo(404)));
        }
        assertEquals("ok", circuitos.llamar("confiteria", () -> "ok"));
    }

    @Test
    void cada_servicio_tiene_su_propio_circuito() {
        for (int i = 0; i < 5; i++) {
            assertThrows(FeignException.class, () -> circuitos.llamar("confiteria", this::caido));
        }
        assertEquals("ok", circuitos.llamar("cartelera", () -> "ok"));
    }

    private String caido() {
        llamadas.incrementAndGet();
        throw error(503);
    }

    private static String rechazo(int status) {
        throw error(status);
    }

    private static FeignException error(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://servicio/api", Map.of(), null,
                StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("llamada", Response.builder()
                .status(status).reason("error").request(request).headers(Map.of())
                .body("{}", StandardCharsets.UTF_8).build());
    }
}
