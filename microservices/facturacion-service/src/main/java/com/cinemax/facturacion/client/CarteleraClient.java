package com.cinemax.facturacion.client;

import com.cinemax.facturacion.dto.external.ShowtimeDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "cartelera-service")
public interface CarteleraClient {

    // Ruta real confirmada en ShowtimeController: /api/showtimes/{id} (sin /v1, el gateway reescribe internamente)
    // idShowtime es String porque cartelera-service usa MongoDB (ObjectId), no un id numerico autoincremental.
    @GetMapping("/api/showtimes/{id}")
    ShowtimeDTO getShowtime(@PathVariable("id") String id);

    // Igual que el monolito: showtime.setAvailableSeats(availableSeats - seats.size()) al confirmar una venta.
    // Ruta interna: no pasa por el gateway y exige el token de servicio.
    @PostMapping("/internal/showtimes/{id}/decrease-seats")
    void decreaseAvailableSeats(@PathVariable("id") String id, @RequestParam("cantidad") int cantidad);
}