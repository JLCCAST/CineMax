package com.cinemax.facturacion.dto.external;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Respuesta de GET /internal/showtimes/{id}/contexto-venta (cartelera-service). */
@Data
public class SaleContextDTO {
    private String idShowtime;
    private String status;
    private LocalDate showDate;
    private LocalTime startTime;
    private Integer idRoom;
    private Integer idVenue;
    private String movieId;
    private LocalDate releaseDate;
    private List<Fare> fares;

    @Data
    public static class Fare {
        private String categoryCode;
        private String categoryName;
        private BigDecimal price;
    }
}
