package com.cinemax.facturacion.service;

import com.cinemax.facturacion.client.CarteleraClient;
import com.cinemax.facturacion.client.SucursalesClient;
import com.cinemax.facturacion.config.RelojConfig;
import com.cinemax.facturacion.dto.external.RoomVenueDTO;
import com.cinemax.facturacion.dto.external.SeatDTO;
import com.cinemax.facturacion.dto.external.ShowtimeDTO;
import com.cinemax.facturacion.dto.response.SaleTransactionHistoryDTO;
import com.cinemax.facturacion.model.entity.SaleSnackDetail;
import com.cinemax.facturacion.model.entity.SaleTicketDetail;
import com.cinemax.facturacion.model.entity.SaleTransaction;
import com.cinemax.facturacion.repository.SaleSnackDetailRepository;
import com.cinemax.facturacion.repository.SaleTicketDetailRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * "Mis compras". Desde V6 la venta guarda película, función, sala, sede, butacas y
 * snacks, así que el historial se arma con dos consultas locales y sin llamar a
 * otros servicios. Solo las ventas anteriores a V6 se completan con cartelera y
 * sucursales, una vez por venta.
 */
@Component
@RequiredArgsConstructor
public class HistorialCompras {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final SaleTicketDetailRepository saleTicketDetailRepository;
    private final SaleSnackDetailRepository saleSnackDetailRepository;
    private final CarteleraClient carteleraClient;
    private final SucursalesClient sucursalesClient;

    public List<SaleTransactionHistoryDTO> armar(List<SaleTransaction> ventas) {
        if (ventas.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = ventas.stream().map(SaleTransaction::getIdTransaction).toList();
        Map<Integer, List<SaleTicketDetail>> entradas = saleTicketDetailRepository
                .findBySaleTransaction_IdTransactionIn(ids).stream()
                .collect(Collectors.groupingBy(t -> t.getSaleTransaction().getIdTransaction()));
        Map<Integer, List<SaleSnackDetail>> snacks = saleSnackDetailRepository
                .findBySaleTransaction_IdTransactionIn(ids).stream()
                .collect(Collectors.groupingBy(s -> s.getSaleTransaction().getIdTransaction()));
        return ventas.stream()
                .map(v -> item(v, entradas.getOrDefault(v.getIdTransaction(), List.of()),
                        snacks.getOrDefault(v.getIdTransaction(), List.of())))
                .toList();
    }

    private SaleTransactionHistoryDTO item(SaleTransaction venta, List<SaleTicketDetail> entradas,
                                           List<SaleSnackDetail> snacks) {
        SaleTransactionHistoryDTO dto = new SaleTransactionHistoryDTO();
        dto.setIdTransaction(venta.getIdTransaction());
        dto.setTotalAmount(venta.getTotalAmount());
        dto.setStatus(venta.getTransactionStatus().getNameStatus());
        dto.setMovieTitle(venta.getMovieTitle());
        dto.setVenueName(venta.getVenueName());
        dto.setRoomName(venta.getRoomName());
        // Sin función (pedido de confitería): la hora de pago, que la base guarda en UTC.
        LocalDateTime pago = venta.getPaymentDate().atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(RelojConfig.ZONA_CINE).toLocalDateTime();
        LocalDate fecha = venta.getShowDate() != null ? venta.getShowDate() : pago.toLocalDate();
        LocalTime hora = venta.getStartTime() != null ? venta.getStartTime() : pago.toLocalTime();
        dto.setDate(fecha.toString());
        dto.setTime(hora.format(HORA));
        dto.setSeats(entradas.stream()
                .map(t -> t.getSeatCode() != null ? t.getSeatCode() : String.valueOf(t.getIdSeat()))
                .collect(Collectors.joining(", ")));
        dto.setSnacks(snacks.stream()
                .map(s -> s.getQuantity() + "x " + (s.getSnackName() != null ? s.getSnackName() : "snack #" + s.getIdSnack()))
                .toList());
        if (venta.getMovieTitle() == null && !entradas.isEmpty()) {
            completarVentaAntigua(dto, entradas);
        }
        return dto;
    }

    // Ventas anteriores a V6: no guardaron la función. Si ya no existe, se dice así.
    private void completarVentaAntigua(SaleTransactionHistoryDTO dto, List<SaleTicketDetail> entradas) {
        try {
            ShowtimeDTO funcion = carteleraClient.getShowtime(entradas.get(0).getIdShowtime());
            dto.setMovieTitle(funcion.getMovieTitle());
            dto.setDate(funcion.getShowDate());
            dto.setTime(funcion.getStartTime() != null && funcion.getStartTime().length() >= 5
                    ? funcion.getStartTime().substring(0, 5) : funcion.getStartTime());
            if (funcion.getIdRoom() != null) {
                RoomVenueDTO sala = sucursalesClient.getRoomWithVenue(funcion.getIdRoom());
                dto.setRoomName(sala.getNameRoom());
                dto.setVenueName(sala.getNameVenue());
            }
            Map<Integer, SeatDTO> butacas = sucursalesClient
                    .getSeats(entradas.stream().map(SaleTicketDetail::getIdSeat).toList()).stream()
                    .collect(Collectors.toMap(SeatDTO::getIdSeat, Function.identity()));
            dto.setSeats(entradas.stream()
                    .map(t -> butacas.containsKey(t.getIdSeat())
                            ? butacas.get(t.getIdSeat()).getRowName() + butacas.get(t.getIdSeat()).getColumnNumber()
                            : String.valueOf(t.getIdSeat()))
                    .collect(Collectors.joining(", ")));
        } catch (FeignException e) {
            dto.setMovieTitle("Función que ya no está en cartelera");
        }
    }
}
