package com.cinemax.facturacion.service;

import com.cinemax.facturacion.client.CarteleraClient;
import com.cinemax.facturacion.client.ConfiteriaClient;
import com.cinemax.facturacion.dto.external.MovimientoStockDTO;
import com.cinemax.facturacion.promociones.CotizacionService.LineaSnack;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Pasos remotos de una compra: descontar butacas en cartelera y stock en confitería.
 *
 * Cada paso que sale bien deja registrada su compensación. Si después la venta
 * local se deshace (otro paso rechazado, un error al guardar), las compensaciones
 * se ejecutan y devuelven lo que ya se había descontado. Debe llamarse dentro de
 * la transacción de la venta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservasRemotas {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final CarteleraClient carteleraClient;
    private final ConfiteriaClient confiteriaClient;

    public void descontarButacas(String idShowtime, int cantidad) {
        ejecutar(() -> carteleraClient.decreaseAvailableSeats(idShowtime, cantidad));
        alDeshacer("las butacas de la función " + idShowtime,
                () -> carteleraClient.increaseAvailableSeats(idShowtime, cantidad));
    }

    public void descontarStock(Integer idVenue, List<LineaSnack> snacks) {
        if (snacks.isEmpty()) {
            return;
        }
        MovimientoStockDTO movimiento = new MovimientoStockDTO(idVenue, snacks.stream()
                .map(s -> new MovimientoStockDTO.Item(s.idSnack(), s.quantity()))
                .toList());
        ejecutar(() -> confiteriaClient.descontarStock(movimiento));
        alDeshacer("los snacks de la sede " + idVenue, () -> confiteriaClient.reponerStock(movimiento));
    }

    // Un rechazo del otro servicio (409 o 404) le llega al cliente con el mismo código y mensaje.
    private static void ejecutar(Runnable llamada) {
        try {
            llamada.run();
        } catch (FeignException e) {
            if (e.status() == 409 || e.status() == 404) {
                throw new ResponseStatusException(HttpStatus.valueOf(e.status()), mensaje(e));
            }
            throw e;
        }
    }

    private static void alDeshacer(String que, Runnable compensacion) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) {
                    return;
                }
                try {
                    compensacion.run();
                    log.info("Venta deshecha: se devolvieron {}.", que);
                } catch (RuntimeException e) {
                    // No hay a quién más avisar: queda en el log para corregirlo a mano.
                    log.error("Venta deshecha, pero no se pudieron devolver {}: {}", que, e.getMessage());
                }
            }
        });
    }

    private static String mensaje(FeignException e) {
        try {
            JsonNode body = JSON.readTree(e.contentUTF8());
            if (body != null && body.hasNonNull("message")) {
                return body.get("message").asText();
            }
        } catch (JsonProcessingException ignored) {
            // Sin cuerpo legible: se usa el mensaje genérico.
        }
        return "No se pudo completar la compra.";
    }
}
