package com.cinemax.cartelera.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketFareDTO {
    private String categoryCode;
    private String categoryName;
    private BigDecimal price;
}
