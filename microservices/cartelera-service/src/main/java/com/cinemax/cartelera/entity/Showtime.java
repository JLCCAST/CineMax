package com.cinemax.cartelera.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
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
// Las búsquedas de la cartelera: por película, por sede y por sala, siempre con la fecha.
@CompoundIndex(name = "pelicula_fecha", def = "{'movieId': 1, 'showDate': 1}")
@CompoundIndex(name = "sede_fecha", def = "{'idVenue': 1, 'showDate': 1}")
@CompoundIndex(name = "sala_fecha", def = "{'idRoom': 1, 'showDate': 1}")
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

    // Butacas vendidas (sube con cada venta y baja si se devuelve). Las ventas viven en
    // facturación; este contador basta para saber si la función ya vendió entradas.
    private Integer soldSeats;

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