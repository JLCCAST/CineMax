package com.cinemax.facturacion.dto.external;

import lombok.Data;

/**
 * Copia local de lo que necesitamos de cartelera-service, no el DTO completo
 * de ese servicio. Se llena con GET /api/showtimes/{id}.
 *
 * idShowtime es String porque cartelera-service usa MongoDB y expone su _id
 * como ObjectId (string hexadecimal de 24 caracteres). El Angular ya lo maneja
 * como string en toda esta rama; no hay que castear a Integer en ningun lado.
 */
@Data
public class ShowtimeDTO {
    private String idShowtime;
    private String movieTitle;
    private Integer idRoom;
    private Integer idVenue;
    private String status;
    private String showDate;
    private String startTime;
}