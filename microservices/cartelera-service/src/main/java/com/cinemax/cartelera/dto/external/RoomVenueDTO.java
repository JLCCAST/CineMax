package com.cinemax.cartelera.dto.external;

import lombok.Data;

@Data
public class RoomVenueDTO {
    private Integer idRoom;
    private String nameRoom;
    private Integer capacity;
    private Integer idVenue;
    private String nameVenue;
}
