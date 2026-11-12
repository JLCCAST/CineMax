package com.cinemax.facturacion.client;

import com.cinemax.facturacion.dto.external.RoomVenueDTO;
import com.cinemax.facturacion.dto.external.SeatDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "sucursales-service")
public interface SucursalesClient {

    @GetMapping("/internal/seats/{idSeat}")
    SeatDTO getSeat(@PathVariable("idSeat") Integer idSeat);

    // Las butacas de una compra, con su sala, en una sola llamada.
    @GetMapping("/internal/seats")
    List<SeatDTO> getSeats(@RequestParam("ids") List<Integer> ids);

    @GetMapping("/internal/rooms/{idRoom}")
    RoomVenueDTO getRoomWithVenue(@PathVariable("idRoom") Integer idRoom);

    @GetMapping("/internal/rooms/{idRoom}/seats")
    List<SeatDTO> getSeatsByRoom(@PathVariable("idRoom") Integer idRoom);
}