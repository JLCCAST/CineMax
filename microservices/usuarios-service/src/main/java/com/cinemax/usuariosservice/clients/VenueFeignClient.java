package com.cinemax.usuariosservice.clients;

import com.cinemax.usuariosservice.model.dto.venue.VenueDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "sucursales-service")
public interface VenueFeignClient {

    // Ruta interna: no depende de los permisos del usuario que hace la petición.
    @GetMapping("/internal/venues")
    List<VenueDTO> getVenues(@RequestParam("status") String status);
}