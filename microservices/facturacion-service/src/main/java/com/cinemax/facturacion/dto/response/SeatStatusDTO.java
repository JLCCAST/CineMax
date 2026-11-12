package com.cinemax.facturacion.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusDTO {
    private Integer idSeat;
    private String rowLetter;
    private Integer columnNumber;
    private String status;
    private String nameSeatType;
    private Boolean isOccupied;
}
