package com.cinemax.cartelera.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "showtimes")
public class Showtime {

    @Id
    private String idShowtime;

    private LocalDate showDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String languageFormat;

    @Min(value = 0, message = "El precio base no puede ser negativo")
    private BigDecimal baseTicketPrice;

    private Integer availableSeats;

    @Builder.Default
    @Pattern(regexp = "Programada|En Curso|Finalizada|Cancelada", message = "El estado debe ser: Programada, En Curso, Finalizada o Cancelada")
    private String status = "Programada";

    // Antes era @ManyToOne a Movie. Ahora solo el id.
    private String movieId;

    // Antes era @ManyToOne a Room (de otro microservicio). Se guarda plano.
    private Integer idRoom;

    // Sede de la sala. Se copia de sucursales al crear o editar la función.
    private Integer idVenue;
}