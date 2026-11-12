package com.cinemax.facturacion.service;

import com.cinemax.facturacion.client.CarteleraClient;
import com.cinemax.facturacion.client.ConfiteriaClient;
import com.cinemax.facturacion.client.SucursalesClient;
import com.cinemax.facturacion.client.UsuariosClient;
import com.cinemax.facturacion.dto.external.RoomVenueDTO;
import com.cinemax.facturacion.dto.external.SeatDTO;
import com.cinemax.facturacion.dto.external.ShowtimeDTO;
import com.cinemax.facturacion.dto.external.SnackStockDTO;
import com.cinemax.facturacion.dto.external.UserDTO;
import com.cinemax.facturacion.dto.request.SaleTransactionRequestDTO;
import com.cinemax.facturacion.dto.response.SaleTransactionHistoryDTO;
import com.cinemax.facturacion.dto.response.SaleTransactionResponseDTO;
import com.cinemax.facturacion.model.entity.SaleSnackDetail;
import com.cinemax.facturacion.model.entity.SaleTicketDetail;
import com.cinemax.facturacion.model.entity.SaleTransaction;
import com.cinemax.facturacion.model.entity.TransactionStatus;
import com.cinemax.facturacion.promociones.CotizacionService;
import com.cinemax.facturacion.promociones.CotizacionService.Cotizacion;
import com.cinemax.facturacion.promociones.CotizacionService.EntradaPedida;
import com.cinemax.facturacion.promociones.CotizacionService.LineaEntrada;
import com.cinemax.facturacion.promociones.CotizacionService.LineaSnack;
import com.cinemax.facturacion.promociones.CotizacionService.SnackPedido;
import com.cinemax.facturacion.repository.SaleSnackDetailRepository;
import com.cinemax.facturacion.repository.SaleTicketDetailRepository;
import com.cinemax.facturacion.repository.SaleTransactionRepository;
import com.cinemax.facturacion.repository.TransactionStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleTransactionServiceImpl implements SaleTransactionService {

    private final SaleTransactionRepository saleTransactionRepository;
    private final SaleTicketDetailRepository saleTicketDetailRepository;
    private final SaleSnackDetailRepository saleSnackDetailRepository;
    private final TransactionStatusRepository transactionStatusRepository;
    private final CotizacionService cotizacionService;

    private final CarteleraClient carteleraClient;
    private final ConfiteriaClient confiteriaClient;
    private final UsuariosClient usuariosClient;
    private final SucursalesClient sucursalesClient;

    @Override
    @Transactional
    public SaleTransactionResponseDTO createSaleTransaction(SaleTransactionRequestDTO request) {
        UserDTO user = usuariosClient.getProfile();
        if (user == null) {
            throw new IllegalStateException("Usuario no encontrado.");
        }

        // Precios, promoción y descuentos los calcula el servidor.
        Cotizacion cotizacion = cotizacionService.cotizar(
                request.getIdShowtime(), entradasPedidas(request), snacksPedidos(request),
                request.getPromotionCode(), user);

        List<Integer> seatIds = request.getAsientosIds() != null ? request.getAsientosIds() : List.of();
        if (seatIds.size() != cotizacion.entradas().size()) {
            throw new IllegalStateException("La cantidad de tickets no coincide con los asientos seleccionados.");
        }
        validarButacas(request.getIdShowtime(), seatIds);
        verificarTotalEsperado(request.getTotalAmount(), cotizacion.total());

        SaleTransaction transaction = registrarVenta(user, request.getPaymentMethod(), cotizacion);
        registrarEntradas(transaction, request.getIdShowtime(), seatIds, cotizacion.entradas());
        registrarSnacks(transaction, cotizacion);
        carteleraClient.decreaseAvailableSeats(request.getIdShowtime(), seatIds.size());

        return SaleTransactionResponseDTO.builder()
                .idTransaction(transaction.getIdTransaction())
                .qrCodeData(transaction.getQrCodeData())
                .message("Reserva confirmada exitosamente.")
                .subtotal(cotizacion.subtotal())
                .discountAmount(cotizacion.descuento())
                .totalAmount(cotizacion.total())
                .namePromotion(cotizacion.promocion() != null ? cotizacion.promocion().getNamePromotion() : null)
                .build();
    }

    private static List<EntradaPedida> entradasPedidas(SaleTransactionRequestDTO request) {
        return request.getTickets() == null ? List.of() : request.getTickets().stream()
                .map(t -> new EntradaPedida(t.getCategoryCode(), t.getCantidad() == null ? 0 : t.getCantidad()))
                .toList();
    }

    private static List<SnackPedido> snacksPedidos(SaleTransactionRequestDTO request) {
        return request.getSnacks() == null ? List.of() : request.getSnacks().stream()
                .map(s -> new SnackPedido(s.getIdSnack(), s.getCantidad() == null ? 0 : s.getCantidad()))
                .toList();
    }

    // Que cada butaca exista, no esté en mantenimiento ni vendida para esta función.
    private void validarButacas(String idShowtime, List<Integer> seatIds) {
        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new IllegalStateException("Hay butacas repetidas en la compra.");
        }
        for (Integer idSeat : seatIds) {
            SeatDTO seat = sucursalesClient.getSeat(idSeat);
            if (seat == null) {
                throw new IllegalStateException("Asiento ID " + idSeat + " no encontrado.");
            }
            String codigo = seat.getRowName() + seat.getColumnNumber();
            if ("MANTENIMIENTO".equals(seat.getStatus())) {
                throw new IllegalStateException("El asiento " + codigo + " está en mantenimiento.");
            }
            if (saleTicketDetailRepository.existsByIdShowtimeAndIdSeat(idShowtime, idSeat)) {
                throw new IllegalStateException("El asiento " + codigo + " ya fue reservado.");
            }
        }
    }

    // El cliente envía el total que vio; si cambió (otra promoción, otro precio), se le avisa.
    private static void verificarTotalEsperado(BigDecimal esperado, BigDecimal calculado) {
        if (esperado != null && esperado.compareTo(calculado) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El precio cambió: el total ahora es S/ " + calculado + ".");
        }
    }

    private SaleTransaction registrarVenta(UserDTO user, String paymentMethod, Cotizacion cotizacion) {
        TransactionStatus completado = transactionStatusRepository.findByNameStatus("COMPLETADO")
                .orElseThrow(() -> new IllegalStateException("Estado COMPLETADO no configurado."));
        String qrCodeData = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        return saleTransactionRepository.save(SaleTransaction.builder()
                .idUser(user.getIdUser())
                .promotion(cotizacion.promocion())
                .transactionStatus(completado)
                .subtotal(cotizacion.subtotal())
                .discountAmount(cotizacion.descuento())
                .totalAmount(cotizacion.total())
                .paymentMethod(paymentMethod)
                .paymentDate(LocalDateTime.now())
                .qrCodeData(qrCodeData)
                .build());
    }

    private void registrarEntradas(SaleTransaction transaction, String idShowtime,
                                   List<Integer> seatIds, List<LineaEntrada> entradas) {
        for (int i = 0; i < seatIds.size(); i++) {
            LineaEntrada entrada = entradas.get(i);
            saleTicketDetailRepository.save(SaleTicketDetail.builder()
                    .saleTransaction(transaction)
                    .idShowtime(idShowtime)
                    .idSeat(seatIds.get(i))
                    .fareCategory(entrada.categoryCode())
                    .ticketPrice(entrada.price())
                    .discountAmount(entrada.discount())
                    .isUsed(false)
                    .build());
        }
    }

    // Descuenta el stock de la sede de la función. idSnackVenueStock (el registro)
    // es distinto de idSnack (el producto): se busca por snack y sede.
    private void registrarSnacks(SaleTransaction transaction, Cotizacion cotizacion) {
        Integer idVenue = cotizacion.contexto().getIdVenue();
        for (LineaSnack linea : cotizacion.snacks()) {
            SnackStockDTO stock = confiteriaClient.findBySnack(linea.idSnack()).stream()
                    .filter(r -> Objects.equals(r.getIdVenue(), idVenue))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "El snack " + linea.idSnack() + " no está disponible en esta sede."));

            if (stock.getStock() < linea.quantity()) {
                throw new IllegalStateException("Stock insuficiente para el snack " + linea.idSnack());
            }
            stock.setStock(stock.getStock() - linea.quantity());
            confiteriaClient.updateStock(stock.getIdSnackVenueStock(), stock);

            saleSnackDetailRepository.save(SaleSnackDetail.builder()
                    .saleTransaction(transaction)
                    .idSnack(linea.idSnack())
                    .quantity(linea.quantity())
                    .unitPrice(linea.unitPrice())
                    .discountAmount(linea.discount())
                    .isDelivered(false)
                    .build());
        }
    }

    @Override
    public List<SaleTransactionHistoryDTO> getMyPurchases() {
        UserDTO user = usuariosClient.getProfile();
        List<SaleTransaction> transactions = saleTransactionRepository.findByIdUserOrderByPaymentDateDesc(user.getIdUser());

        return transactions.stream().map(tx -> {
            SaleTransactionHistoryDTO dto = new SaleTransactionHistoryDTO();
            dto.setIdTransaction(tx.getIdTransaction());
            dto.setTotalAmount(tx.getTotalAmount());
            dto.setStatus(tx.getTransactionStatus().getNameStatus());

            List<SaleTicketDetail> tickets = saleTicketDetailRepository.findBySaleTransaction_IdTransaction(tx.getIdTransaction());

            if (!tickets.isEmpty()) {
                String idShowtime = tickets.get(0).getIdShowtime();
                try {
                    ShowtimeDTO showtime = carteleraClient.getShowtime(idShowtime);
                    dto.setMovieTitle(showtime.getMovieTitle());
                    dto.setDate(showtime.getShowDate());
                    dto.setTime(showtime.getStartTime());

                    if (showtime.getIdRoom() != null) {
                        try {
                            RoomVenueDTO roomVenue = sucursalesClient.getRoomWithVenue(showtime.getIdRoom());
                            dto.setRoomName(roomVenue.getNameRoom());
                            dto.setVenueName(roomVenue.getNameVenue());
                        } catch (Exception e) {
                            dto.setRoomName(null);
                            dto.setVenueName(null);
                        }
                    }
                } catch (Exception e) {
                    dto.setMovieTitle(null);
                    dto.setDate(null);
                    dto.setTime(null);
                }

                // Igual que en createSaleTransaction: mostrar el código real del asiento
                // (fila+columna) en vez del ID crudo. Como aquí no hay validación de estado
                // pendiente (la compra ya se hizo), basta con resolverlo para mostrarlo.
                dto.setSeats(tickets.stream()
                        .map(t -> {
                            try {
                                SeatDTO seat = sucursalesClient.getSeat(t.getIdSeat());
                                return seat.getRowName() + seat.getColumnNumber();
                            } catch (Exception e) {
                                return String.valueOf(t.getIdSeat());
                            }
                        })
                        .collect(Collectors.joining(", ")));
            } else {
                dto.setDate(tx.getPaymentDate().toLocalDate().toString());
                dto.setTime(tx.getPaymentDate().toLocalTime().toString());
            }

            List<SaleSnackDetail> snacks = saleSnackDetailRepository.findBySaleTransaction_IdTransaction(tx.getIdTransaction());
            dto.setSnacks(snacks.stream()
                    .map(s -> s.getQuantity() + "x snack #" + s.getIdSnack())
                    .collect(Collectors.toList()));

            return dto;
        }).collect(Collectors.toList());
    }
}