package com.cinemax.facturacion.client;

import com.cinemax.facturacion.dto.external.SnackInfoDTO;
import com.cinemax.facturacion.dto.external.SnackStockDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "confiteria-service")
public interface ConfiteriaClient {

    @GetMapping("/api/snack-venue-stock")
    List<SnackStockDTO> findBySnack(@RequestParam("snackId") Integer snackId);

    // Precio y categoría vigentes, para cobrar con datos del servidor.
    @GetMapping("/internal/snacks")
    List<SnackInfoDTO> getSnacks(@RequestParam("ids") List<Integer> ids);

    // Ruta interna: no pasa por el gateway y exige el token de servicio.
    @PutMapping("/internal/snack-venue-stock/{idSnackVenueStock}")
    SnackStockDTO updateStock(@PathVariable("idSnackVenueStock") Integer idSnackVenueStock,
                                @RequestBody SnackStockDTO body);
}