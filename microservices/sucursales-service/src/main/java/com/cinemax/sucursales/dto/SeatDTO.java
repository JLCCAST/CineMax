package com.cinemax.sucursales.dto;

import lombok.Data;

@Data
public class SeatDTO {
    private Integer idSeat;
    private String rowName;
    private Integer columnNumber;
    private String status;
    private Integer idSeatType;
    private String nameSeatType;
    private Integer idRoom;
}