package com.cinemax.sucursales.controller;

import com.cinemax.sucursales.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/venues")
@RequiredArgsConstructor
public class InternalVenueController {

    private final VenueRepository venueRepository;

    // Usado por facturacion-service en los pedidos solo de confitería: comprueba la
    // sede de recojo y guarda su nombre en la venta.
    @GetMapping("/{idVenue}")
    public ResponseEntity<Map<String, Object>> getVenue(@PathVariable Integer idVenue) {
        return venueRepository.findById(idVenue)
                .map(venue -> ResponseEntity.ok(Map.<String, Object>of(
                        "idVenue", venue.getIdVenue(),
                        "nameVenue", venue.getNameVenue(),
                        "status", venue.getStatus())))
                .orElse(ResponseEntity.notFound().build());
    }
}
