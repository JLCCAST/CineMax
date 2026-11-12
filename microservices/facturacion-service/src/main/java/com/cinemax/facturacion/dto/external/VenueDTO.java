package com.cinemax.facturacion.dto.external;

import lombok.Data;

// Sede de recojo de un pedido solo de confitería (sucursales-service).
@Data
public class VenueDTO {
    private Integer idVenue;
    private String nameVenue;
    private String status;
}
