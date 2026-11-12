package com.cinemax.cartelera.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeResponseDTO {

    private String idShowtime;

    private LocalDate showDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String languageFormat;

    private BigDecimal baseTicketPrice;

    private Integer availableSeats;

    private String status;

    // Info resumida de la película, no todo el objeto completo
    private String movieId;
    private String movieTitle;

    private Integer idRoom;
    private String nameRoom;

    private Integer idVenue;
    private String nameVenue;
}