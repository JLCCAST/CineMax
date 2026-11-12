package com.cinemax.sucursales.controller;

import com.cinemax.sucursales.dto.RoomVenueDTO;
import com.cinemax.sucursales.dto.SeatDTO;
import com.cinemax.sucursales.entity.Room;
import com.cinemax.sucursales.entity.Seat;
import com.cinemax.sucursales.repository.RoomRepository;
import com.cinemax.sucursales.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/rooms")
@RequiredArgsConstructor
public class InternalRoomController {

    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;

    // Usado por facturacion-service para mostrar sala + sucursal en el historial de compras
    // (showtime.idRoom viene de cartelera-service, pero el nombre real de la sala y la
    // sucursal viven acá).
    @GetMapping("/{idRoom}")
    public ResponseEntity<RoomVenueDTO> getRoomWithVenue(@PathVariable Integer idRoom) {
        return roomRepository.findById(idRoom)
                .map(room -> ResponseEntity.ok(toDTO(room)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Usado por cartelera-service para poner nombre de sala y sede a una lista de funciones.
    @GetMapping
    public ResponseEntity<List<RoomVenueDTO>> getRoomsWithVenue(@RequestParam List<Integer> ids) {
        return ResponseEntity.ok(roomRepository.findAllById(ids).stream().map(this::toDTO).toList());
    }

    // Usado por facturacion-service para armar el mapa de butacas de una función.
    @GetMapping("/{idRoom}/seats")
    public ResponseEntity<List<SeatDTO>> getSeatsByRoom(@PathVariable Integer idRoom) {
        if (!roomRepository.existsById(idRoom)) {
            return ResponseEntity.notFound().build();
        }
        List<SeatDTO> seats = seatRepository.findByRoom_IdRoomOrderByRowNameAscColumnNumberAsc(idRoom)
                .stream()
                .map(this::toSeatDTO)
                .toList();
        return ResponseEntity.ok(seats);
    }

    private RoomVenueDTO toDTO(Room room) {
        RoomVenueDTO dto = new RoomVenueDTO();
        dto.setIdRoom(room.getIdRoom());
        dto.setNameRoom(room.getNameRoom());
        dto.setCapacity(room.getCapacity());
        if (room.getVenue() != null) {
            dto.setIdVenue(room.getVenue().getIdVenue());
            dto.setNameVenue(room.getVenue().getNameVenue());
        }
        return dto;
    }

    private SeatDTO toSeatDTO(Seat seat) {
        SeatDTO dto = new SeatDTO();
        dto.setIdSeat(seat.getIdSeat());
        dto.setRowName(seat.getRowName());
        dto.setColumnNumber(seat.getColumnNumber());
        dto.setStatus(seat.getStatus());
        if (seat.getSeatType() != null) {
            dto.setIdSeatType(seat.getSeatType().getIdSeatType());
            dto.setNameSeatType(seat.getSeatType().getNameSeatType());
        }
        return dto;
    }
}
