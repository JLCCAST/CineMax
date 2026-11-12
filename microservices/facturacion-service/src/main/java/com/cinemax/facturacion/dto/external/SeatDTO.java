package com.cinemax.facturacion.dto.external;

import lombok.Data;

@Data
public class SeatDTO {
    private Integer idSeat;
    private String rowName;
    private Integer columnNumber;
    private String status;
    private String nameSeatType;
}