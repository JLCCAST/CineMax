package com.cinemax.cartelera.dto;

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
public class ShowtimeSummaryDTO {
    private String idShowtime;
    private String titleMovie;
    private String posterUrl;
    private Integer idRoom;
    private String nameRoom;
    private Integer idVenue;
    private String nameVenue;
    private LocalDate showDate;
    private LocalTime startTime;
    private String languageFormat;
}
