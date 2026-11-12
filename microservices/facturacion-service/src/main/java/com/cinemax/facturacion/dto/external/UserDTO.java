package com.cinemax.facturacion.dto.external;

import lombok.Data;

import java.time.LocalDate;

/**
 * Copia local de lo que necesitamos del usuario autenticado.
 * Se llena con la respuesta de GET /api/v1/users/profile (usuarios-service),
 * reenviando el JWT original (ver FeignConfig).
 */
@Data
public class UserDTO {
    private Integer idUser;
    private String email;
    private String firstName;
    private Integer idVenue;
    private LocalDate datebirth;
}