package com.cinemax.cartelera.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.cinemax.cartelera.dto.external.RoomVenueDTO;

@FeignClient(name = "sucursales-service")
public interface SucursalesClient {

    @GetMapping("/internal/rooms/{idRoom}")
    RoomVenueDTO getRoomWithVenue(@PathVariable("idRoom") Integer idRoom);

    @GetMapping("/internal/rooms")
    List<RoomVenueDTO> getRoomsWithVenue(@RequestParam("ids") List<Integer> ids);
}
