package com.cinemax.cartelera.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinemax.cartelera.dto.SaleContextDTO;
import com.cinemax.cartelera.service.ShowtimeService;

import lombok.RequiredArgsConstructor;

/**
 * Operaciones que solo usan otros servicios. El gateway no enruta /internal/**
 * y ServiceAccessFilter exige el token de servicio.
 */
@RestController
@RequestMapping("/internal/showtimes")
@RequiredArgsConstructor
public class InternalShowtimeController {

    private final ShowtimeService showtimeService;

    // Llamado por facturacion-service para cotizar una compra.
    @GetMapping("/{id}/contexto-venta")
    public ResponseEntity<SaleContextDTO> getSaleContext(@PathVariable String id) {
        return ResponseEntity.ok(showtimeService.getSaleContext(id));
    }

    // Llamado por facturacion-service al confirmar una venta.
    // Es POST y no PATCH porque el cliente HTTP por defecto de Feign
    // (HttpURLConnection) no admite PATCH.
    @PostMapping("/{id}/decrease-seats")
    public ResponseEntity<Void> decreaseAvailableSeats(@PathVariable String id, @RequestParam int cantidad) {
        showtimeService.decreaseAvailableSeats(id, cantidad);
        return ResponseEntity.noContent().build();
    }
}
