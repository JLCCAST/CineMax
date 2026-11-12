package com.cinemax.confiteria.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnackResponseDTO {

    private Integer idSnack;

    private Integer idVenue;

    private SnackCategoryResponseDTO snackCategory;
    private Integer idSnackCategory;
    private String nameCategory;

    private String nameSnack;

    private String descriptionSnack;

    private BigDecimal price;

    private Integer stock;

    private String imageUrlSnack;

    private String status;

    private LocalDateTime createdAt;
}