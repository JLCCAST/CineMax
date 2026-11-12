package com.cinemax.facturacion.dto.request;

import lombok.Data;

import java.util.List;

// Lo que el cliente quiere comprar. Los precios los pone el servidor.
@Data
public class CotizacionRequestDTO {
    private String idShowtime;
    private List<Ticket> tickets;
    private List<Snack> snacks;
    private String promotionCode;

    @Data
    public static class Ticket {
        private String categoryCode;
        private Integer cantidad;
    }

    @Data
    public static class Snack {
        private Integer idSnack;
        private Integer cantidad;
    }
}
