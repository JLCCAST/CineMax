package com.cinemax.sucursales.controller;

import com.cinemax.sucursales.dto.SeatDTO;
import com.cinemax.sucursales.entity.Seat;
import com.cinemax.sucursales.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/seats")
@RequiredArgsConstructor
public class InternalSeatController {

    private final SeatRepository seatRepository;

    @GetMapping("/{idSeat}")
    public ResponseEntity<SeatDTO> getSeat(@PathVariable Integer idSeat) {
        return seatRepository.findById(idSeat)
                .map(seat -> ResponseEntity.ok(toDTO(seat)))
                .orElseThrow(() -> new RuntimeException("Asiento ID " + idSeat + " no encontrado."));
    }

    // Usado por facturacion-service para validar las butacas de una compra en una sola llamada.
    // Los ids que no existen simplemente no aparecen en la respuesta.
    @GetMapping
    public ResponseEntity<List<SeatDTO>> getSeats(@RequestParam List<Integer> ids) {
        return ResponseEntity.ok(seatRepository.findAllById(ids).stream().map(this::toDTO).toList());
    }

    private SeatDTO toDTO(Seat seat) {
        SeatDTO dto = new SeatDTO();
        dto.setIdSeat(seat.getIdSeat());
        dto.setRowName(seat.getRowName());
        dto.setColumnNumber(seat.getColumnNumber());
        dto.setStatus(seat.getStatus());
        dto.setIdRoom(seat.getRoom().getIdRoom());
        return dto;
    }
}
