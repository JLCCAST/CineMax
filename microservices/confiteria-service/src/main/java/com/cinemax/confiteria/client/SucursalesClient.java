package com.cinemax.confiteria.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.cinemax.confiteria.dto.external.VenueDTO;

@FeignClient(name = "sucursales-service")
public interface SucursalesClient {

    @GetMapping("/api/venues/public")
    List<VenueDTO> getVenues();
}
