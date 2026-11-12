package com.cinemax.facturacion.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// Los precios, el descuento y la promoción los calcula el servidor. De los
// montos del cliente solo se usa totalAmount, como total esperado: si no
// coincide con el calculado, la compra responde 409.
@Data
public class SaleTransactionRequestDTO {

    private String idShowtime;
    // Solo en un pedido de confitería sin función: la sede donde se recoge.
    private Integer idVenue;
    private List<Integer> asientosIds;
    private List<TicketLineDTO> tickets;
    private List<SnackLineDTO> snacks;

    private BigDecimal totalAmount;

    private String paymentMethod;

    private String promotionCode;

    @Data
    public static class TicketLineDTO {
        private String categoryCode;
        private Integer cantidad;
    }

    @Data
    public static class SnackLineDTO {
        private Integer idSnack;
        private Integer cantidad;
    }
}
