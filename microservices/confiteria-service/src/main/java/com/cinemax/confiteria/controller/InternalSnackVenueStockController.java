package com.cinemax.confiteria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinemax.confiteria.dto.MovimientoStockDTO;
import com.cinemax.confiteria.service.SnackVenueStockService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Operaciones que solo usan otros servicios. El gateway no enruta /internal/**
 * y ServiceAccessFilter exige el token de servicio.
 */
@RestController
@RequestMapping("/internal/snack-venue-stock")
@RequiredArgsConstructor
public class InternalSnackVenueStockController {

    private final SnackVenueStockService snackVenueStockService;

    // Llamado por facturacion-service al vender. Responde 409 si algún snack no alcanza.
    @PostMapping("/descontar")
    public ResponseEntity<Void> descontar(@Valid @RequestBody MovimientoStockDTO movimiento) {
        snackVenueStockService.descontarLote(movimiento);
        return ResponseEntity.noContent().build();
    }

    // Llamado por facturacion-service si la venta no se completó.
    @PostMapping("/reponer")
    public ResponseEntity<Void> reponer(@Valid @RequestBody MovimientoStockDTO movimiento) {
        snackVenueStockService.reponerLote(movimiento);
        return ResponseEntity.noContent().build();
    }
}
