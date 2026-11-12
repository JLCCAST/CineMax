package com.cinemax.confiteria.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Snacks de una compra en una sede: facturación los descuenta al vender
// y los repone si la venta no se completa.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoStockDTO {

    @NotNull
    private Integer idVenue;

    @NotEmpty
    @Valid
    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @NotNull
        private Integer idSnack;

        @NotNull
        @Min(1)
        private Integer cantidad;
    }
}
