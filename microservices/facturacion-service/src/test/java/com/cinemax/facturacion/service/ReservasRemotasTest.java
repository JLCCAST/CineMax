package com.cinemax.facturacion.service;

import com.cinemax.facturacion.client.CarteleraClient;
import com.cinemax.facturacion.client.Circuitos;
import com.cinemax.facturacion.client.ConfiteriaClient;
import com.cinemax.facturacion.dto.external.MovimientoStockDTO;
import com.cinemax.facturacion.promociones.CotizacionService.LineaSnack;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ReservasRemotasTest {

    private static final String FUNCION = "6af5d5629ff5600932abb583";
    private static final int SEDE = 1;

    private final CarteleraClient cartelera = mock(CarteleraClient.class);
    private final ConfiteriaClient confiteria = mock(ConfiteriaClient.class);
    private final ReservasRemotas reservas = new ReservasRemotas(cartelera, confiteria, new Circuitos());

    @BeforeEach
    void abrirTransaccion() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void cerrarTransaccion() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void si_la_venta_se_deshace_devuelve_las_butacas_y_el_stock() {
        reservas.descontarButacas(FUNCION, 2);
        reservas.descontarStock(SEDE, List.of(snack(3, 1)));

        terminar(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(cartelera).increaseAvailableSeats(FUNCION, 2);
        verify(confiteria).reponerStock(movimiento(3, 1));
    }

    @Test
    void si_la_venta_se_confirma_no_devuelve_nada() {
        reservas.descontarButacas(FUNCION, 2);
        reservas.descontarStock(SEDE, List.of(snack(3, 1)));

        terminar(TransactionSynchronization.STATUS_COMMITTED);

        verify(cartelera, never()).increaseAvailableSeats(anyString(), anyInt());
        verify(confiteria, never()).reponerStock(any());
    }

    @Test
    void si_falta_stock_responde_409_y_solo_devuelve_las_butacas() {
        doThrow(rechazo(409, "No hay stock suficiente de Gaseosa Grande en esta sede."))
                .when(confiteria).descontarStock(any());
        reservas.descontarButacas(FUNCION, 1);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> reservas.descontarStock(SEDE, List.of(snack(1, 5))));
        terminar(TransactionSynchronization.STATUS_ROLLED_BACK);

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("No hay stock suficiente de Gaseosa Grande en esta sede.", error.getReason());
        verify(cartelera).increaseAvailableSeats(FUNCION, 1);
        verify(confiteria, never()).reponerStock(any());
    }

    @Test
    void si_no_quedan_butacas_no_hay_nada_que_devolver() {
        doThrow(rechazo(409, "No quedan suficientes butacas en esta función."))
                .when(cartelera).decreaseAvailableSeats(FUNCION, 3);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> reservas.descontarButacas(FUNCION, 3));
        terminar(TransactionSynchronization.STATUS_ROLLED_BACK);

        assertEquals("No quedan suficientes butacas en esta función.", error.getReason());
        verify(cartelera, never()).increaseAvailableSeats(anyString(), anyInt());
    }

    @Test
    void una_compra_sin_snacks_no_llama_a_confiteria() {
        reservas.descontarStock(SEDE, List.of());
        terminar(TransactionSynchronization.STATUS_ROLLED_BACK);
        verifyNoInteractions(confiteria);
    }

    @Test
    void un_servicio_caido_no_se_disfraza_de_rechazo() {
        doThrow(rechazo(503, "Service Unavailable")).when(cartelera).decreaseAvailableSeats(FUNCION, 1);
        assertThrows(FeignException.class, () -> reservas.descontarButacas(FUNCION, 1));
    }

    @Test
    void si_la_compensacion_falla_no_tapa_el_error_de_la_venta() {
        doThrow(new RuntimeException("cartelera no responde"))
                .when(cartelera).increaseAvailableSeats(FUNCION, 1);
        reservas.descontarButacas(FUNCION, 1);

        assertDoesNotThrow(() -> terminar(TransactionSynchronization.STATUS_ROLLED_BACK));
    }

    private static void terminar(int estado) {
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(estado));
    }

    private static LineaSnack snack(int idSnack, int cantidad) {
        return new LineaSnack(idSnack, "Snack " + idSnack, new BigDecimal("8.50"), cantidad, BigDecimal.ZERO);
    }

    private static MovimientoStockDTO movimiento(int idSnack, int cantidad) {
        return new MovimientoStockDTO(SEDE, List.of(new MovimientoStockDTO.Item(idSnack, cantidad)));
    }

    private static FeignException rechazo(int status, String mensaje) {
        Request request = Request.create(Request.HttpMethod.POST, "http://servicio/internal",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder()
                .status(status)
                .reason("rechazo")
                .request(request)
                .headers(Map.of())
                .body("{\"message\":\"" + mensaje + "\"}", StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus("llamada", response);
    }
}
