package com.cinemax.facturacion.dto.external;

import lombok.Data;

import java.math.BigDecimal;

/** Respuesta de GET /internal/snacks (confiteria-service). */
@Data
public class SnackInfoDTO {
    private Integer idSnack;
    private String nameSnack;
    private BigDecimal price;
    private Integer idSnackCategory;
    private String nameCategory;
    private String status;
}
