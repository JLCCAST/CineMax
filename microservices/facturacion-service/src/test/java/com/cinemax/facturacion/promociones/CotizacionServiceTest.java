package com.cinemax.facturacion.promociones;

import com.cinemax.facturacion.client.CarteleraClient;
import com.cinemax.facturacion.client.Circuitos;
import com.cinemax.facturacion.client.ConfiteriaClient;
import com.cinemax.facturacion.dto.external.SaleContextDTO;
import com.cinemax.facturacion.promociones.CotizacionService.Cotizacion;
import com.cinemax.facturacion.promociones.CotizacionService.EntradaPedida;
import com.cinemax.facturacion.repository.PromotionRepository;
import com.cinemax.facturacion.repository.SaleTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CotizacionServiceTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 20);

    private final CarteleraClient cartelera = mock(CarteleraClient.class);
    private final PromotionRepository promociones = mock(PromotionRepository.class);

    @BeforeEach
    void funcionDeLasSeis() {
        SaleContextDTO.Fare adulto = new SaleContextDTO.Fare();
        adulto.setCategoryCode("ADULTO");
        adulto.setCategoryName("Adulto");
        adulto.setPrice(new BigDecimal("25.00"));

        SaleContextDTO contexto = new SaleContextDTO();
        contexto.setIdShowtime("f1");
        contexto.setStatus("Programada");
        contexto.setShowDate(HOY);
        contexto.setStartTime(LocalTime.of(18, 0));
        contexto.setIdRoom(1);
        contexto.setIdVenue(1);
        contexto.setFares(List.of(adulto));
        when(cartelera.getSaleContext(anyString())).thenReturn(contexto);
        when(promociones.findByStatus("Activo")).thenReturn(List.of());
    }

    @Test
    void antes_de_empezar_la_funcion_se_cotiza() {
        Cotizacion cotizacion = servicioALas(17, 59).cotizar("f1", null, List.of(new EntradaPedida("ADULTO", 2)),
                List.of(), null, null);
        assertEquals(new BigDecimal("50.00"), cotizacion.total());
    }

    @Test
    void una_funcion_que_ya_empezo_no_se_vende() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicioALas(18, 0).cotizar("f1", null, List.of(new EntradaPedida("ADULTO", 1)),
                        List.of(), null, null));
        assertEquals("Esta función ya empezó. Elige otro horario.", error.getMessage());
    }

    // El reloj está en hora de Lima: a las 17:59 de Lima ya son las 22:59 UTC.
    private CotizacionService servicioALas(int hora, int minuto) {
        Clock reloj = Clock.fixed(LocalDateTime.of(HOY, LocalTime.of(hora, minuto)).atZone(LIMA).toInstant(), LIMA);
        return new CotizacionService(cartelera, mock(ConfiteriaClient.class), new Circuitos(),
                promociones, mock(SaleTransactionRepository.class), reloj);
    }
}
