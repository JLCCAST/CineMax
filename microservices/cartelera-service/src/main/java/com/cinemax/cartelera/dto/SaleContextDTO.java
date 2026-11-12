package com.cinemax.cartelera.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Lo que facturación necesita de una función para cobrarla: fecha, hora,
// estreno de la película y tarifas.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleContextDTO {
    private String idShowtime;
    private String status;
    private LocalDate showDate;
    private LocalTime startTime;
    private Integer idRoom;
    private Integer idVenue;
    private String movieId;
    private LocalDate releaseDate;
    private List<TicketFareDTO> fares;
}
