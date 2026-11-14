package com.cinemax.confiteria.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SnackRequestDTO {

    private Integer idVenue;

    // El admin la manda como idSnackCategory.
    @NotNull(message = "La categoría es obligatoria")
    @JsonAlias("idSnackCategory")
    private Integer snackCategoryId;

    @NotBlank(message = "El nombre del snack es obligatorio")
    private String nameSnack;

    private String descriptionSnack;

    @NotNull(message = "El precio es obligatorio")
    @Min(value = 0, message = "El precio no puede ser negativo")
    private BigDecimal price;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    private String imageUrlSnack;

    private String status;
}