package com.cinemax.sucursales.controller;

import com.cinemax.sucursales.entity.Venue;
import com.cinemax.sucursales.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/venues")
@RequiredArgsConstructor
public class InternalVenueController {

    private final VenueRepository venueRepository;

    // Usado por usuarios-service: las sedes que el admin puede asignar a un gerente.
    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(required = false) String status) {
        return venueRepository.findAll().stream()
                .filter(venue -> status == null || status.equalsIgnoreCase(venue.getStatus()))
                .sorted(Comparator.comparing(Venue::getIdVenue))
                .map(venue -> Map.<String, Object>of(
                        "idVenue", venue.getIdVenue(),
                        "nameVenue", venue.getNameVenue(),
                        "status", venue.getStatus()))
                .toList();
    }

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
