package com.cinemax.facturacion.service;

import com.cinemax.facturacion.client.CarteleraClient;
import com.cinemax.facturacion.client.Circuitos;
import com.cinemax.facturacion.client.ConfiteriaClient;
import com.cinemax.facturacion.dto.external.MovimientoStockDTO;
import com.cinemax.facturacion.promociones.CotizacionService.LineaSnack;
import com.cinemax.common.error.FeignExceptionHandler;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class ReservasRemotas {

    private final CarteleraClient carteleraClient;
    private final ConfiteriaClient confiteriaClient;
    private final Circuitos circuitos;

    public void descontarButacas(String idShowtime, int cantidad) {
        ejecutar("cartelera", () -> carteleraClient.decreaseAvailableSeats(idShowtime, cantidad));
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
        ejecutar("confiteria", () -> confiteriaClient.descontarStock(movimiento));
        alDeshacer("los snacks de la sede " + idVenue, () -> confiteriaClient.reponerStock(movimiento));
    }

    // Un rechazo del otro servicio (409 o 404) le llega al cliente con el mismo código y mensaje.
    // Los descuentos pasan por el circuit breaker; las devoluciones (alDeshacer) no:
    // si el servicio volvió, hay que intentar devolver aunque el circuito siga abierto.
    private void ejecutar(String servicio, Runnable llamada) {
        try {
            circuitos.ejecutar(servicio, llamada);
        } catch (FeignException e) {
            if (e.status() == 409 || e.status() == 404) {
                throw new ResponseStatusException(HttpStatus.valueOf(e.status()), FeignExceptionHandler.mensajeRemoto(e));
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
}
