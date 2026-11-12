package com.cinemax.facturacion.dto.external;

import java.util.List;

// Snacks de una compra en una sede, para descontarlos o reponerlos en confitería.
public record MovimientoStockDTO(Integer idVenue, List<Item> items) {

    public record Item(Integer idSnack, Integer cantidad) {
    }
}
