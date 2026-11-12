package com.cinemax.facturacion.client;

import com.cinemax.facturacion.dto.external.MovimientoStockDTO;
import com.cinemax.facturacion.dto.external.SnackInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "confiteria-service")
public interface ConfiteriaClient {

    // Precio y categoría vigentes, para cobrar con datos del servidor.
    @GetMapping("/internal/snacks")
    List<SnackInfoDTO> getSnacks(@RequestParam("ids") List<Integer> ids);

    // Resta el stock de todos los snacks de la compra, o de ninguno (409 si alguno no alcanza).
    @PostMapping("/internal/snack-venue-stock/descontar")
    void descontarStock(@RequestBody MovimientoStockDTO movimiento);

    // Compensación: devuelve el stock si la venta no se completó.
    @PostMapping("/internal/snack-venue-stock/reponer")
    void reponerStock(@RequestBody MovimientoStockDTO movimiento);
}
