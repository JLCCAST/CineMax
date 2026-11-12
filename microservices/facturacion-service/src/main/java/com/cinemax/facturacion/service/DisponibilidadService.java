package com.cinemax.facturacion.service;

import com.cinemax.facturacion.client.CarteleraClient;
import com.cinemax.facturacion.client.SucursalesClient;
import com.cinemax.facturacion.dto.external.ShowtimeDTO;
import com.cinemax.facturacion.dto.response.SeatStatusDTO;
import com.cinemax.facturacion.repository.SaleTicketDetailRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Mapa de butacas de una función: la sala viene de cartelera, las butacas de
// sucursales y las vendidas de los tickets de esta base.
@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private final CarteleraClient carteleraClient;
    private final SucursalesClient sucursalesClient;
    private final SaleTicketDetailRepository saleTicketDetailRepository;

    public List<SeatStatusDTO> mapaDeButacas(String idShowtime) {
        ShowtimeDTO showtime;
        try {
            showtime = carteleraClient.getShowtime(idShowtime);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Función no encontrada.");
        }
        if (showtime.getIdRoom() == null) {
            throw new IllegalStateException("La función no tiene sala asignada.");
        }

        Set<Integer> vendidas = new HashSet<>(saleTicketDetailRepository.findIdSeatsByIdShowtime(idShowtime));

        return sucursalesClient.getSeatsByRoom(showtime.getIdRoom()).stream()
                .map(seat -> SeatStatusDTO.builder()
                        .idSeat(seat.getIdSeat())
                        .rowLetter(seat.getRowName())
                        .columnNumber(seat.getColumnNumber())
                        .status(seat.getStatus())
                        .nameSeatType(seat.getNameSeatType())
                        .isOccupied(vendidas.contains(seat.getIdSeat()))
                        .build())
                .toList();
    }
}
