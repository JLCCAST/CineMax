package com.cinemax.confiteria.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinemax.confiteria.dto.SnackVenueStockRequestDTO;
import com.cinemax.confiteria.dto.SnackVenueStockResponseDTO;
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

    // Llamado por facturacion-service para descontar el stock durante la compra.
    // La misma operación en /api/snack-venue-stock/{id} exige MANAGE_CONFITERIA,
    // que un cliente no tiene.
    @PutMapping("/{id}")
    public ResponseEntity<SnackVenueStockResponseDTO> update(
            @PathVariable Integer id, @Valid @RequestBody SnackVenueStockRequestDTO dto) {
        return ResponseEntity.ok(snackVenueStockService.update(id, dto));
    }
}
