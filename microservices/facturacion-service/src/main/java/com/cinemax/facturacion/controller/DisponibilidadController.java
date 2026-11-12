package com.cinemax.facturacion.controller;

import com.cinemax.facturacion.dto.response.SeatStatusDTO;
import com.cinemax.facturacion.service.DisponibilidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    @GetMapping("/showtimes/{idShowtime}/seats")
    public ResponseEntity<List<SeatStatusDTO>> mapaDeButacas(@PathVariable String idShowtime) {
        return ResponseEntity.ok(disponibilidadService.mapaDeButacas(idShowtime));
    }
}
