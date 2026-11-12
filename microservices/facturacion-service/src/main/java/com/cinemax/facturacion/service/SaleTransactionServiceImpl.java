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
import com.cinemax.facturacion.model.entity.Promotion;
import com.cinemax.facturacion.repository.SaleSnackDetailRepository;
import com.cinemax.facturacion.repository.SaleTicketDetailRepository;
import com.cinemax.facturacion.repository.SaleTransactionRepository;
import com.cinemax.facturacion.repository.TransactionStatusRepository;
import com.cinemax.facturacion.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final PromotionRepository promotionRepository;

    private final CarteleraClient carteleraClient;
    private final ConfiteriaClient confiteriaClient;
    private final UsuariosClient usuariosClient;
    private final SucursalesClient sucursalesClient;

    @Override
    @Transactional
    public SaleTransactionResponseDTO createSaleTransaction(SaleTransactionRequestDTO request) {

        // 1. Resolver usuario autenticado (vía JWT reenviado por FeignConfig)
        UserDTO user = usuariosClient.getProfile();
        if (user == null) {
            throw new IllegalStateException("Usuario no encontrado.");
        }

        // 2. Validar showtime (si la compra incluye entradas)
        ShowtimeDTO showtime = null;
        if (request.getIdShowtime() != null) {
            showtime = carteleraClient.getShowtime(request.getIdShowtime());
            if (showtime == null) {
                throw new IllegalStateException("Función no encontrada.");
            }
            if (!"Programada".equals(showtime.getStatus())) {
                throw new IllegalStateException("Esta función ya no está disponible.");
            }
        }

        // 3. Validar asientos: existencia + estado (vía sucursales-service) y que no
        //    estén ya ocupados en esta función (nuestra propia tabla reemplaza "reservar asiento").
        //    Guardamos el SeatDTO de cada uno para mostrar el código real (fila+columna)
        //    en vez del ID crudo, igual que hacía el monolito.
        List<Integer> seatIds = request.getAsientosIds() != null ? request.getAsientosIds() : List.of();
        Map<Integer, SeatDTO> seatsById = new HashMap<>();
        if (showtime != null) {
            for (Integer idSeat : seatIds) {
                SeatDTO seat = sucursalesClient.getSeat(idSeat);
                if (seat == null) {
                    throw new IllegalStateException("Asiento ID " + idSeat + " no encontrado.");
                }
                if ("MANTENIMIENTO".equals(seat.getStatus())) {
                    throw new IllegalStateException(
                            "El asiento " + seat.getRowName() + seat.getColumnNumber() + " está en mantenimiento.");
                }

                boolean yaOcupado = saleTicketDetailRepository
                        .existsByIdShowtimeAndIdSeat(request.getIdShowtime(), idSeat);
                if (yaOcupado) {
                    throw new IllegalStateException(
                            "El asiento " + seat.getRowName() + seat.getColumnNumber() + " ya fue reservado.");
                }

                seatsById.put(idSeat, seat);
            }
        }

        // 4. Expandir "tickets" (agrupados por categoría) a un precio por asiento,
        //    igual que hacía el monolito.
        List<BigDecimal> preciosPorAsiento = new ArrayList<>();
        if (request.getTickets() != null) {
            for (var linea : request.getTickets()) {
                for (int i = 0; i < linea.getCantidad(); i++) {
                    preciosPorAsiento.add(linea.getPrecioUnitario());
                }
            }
        }
        if (preciosPorAsiento.size() != seatIds.size()) {
            throw new IllegalStateException("La cantidad de tickets no coincide con los asientos seleccionados.");
        }

        // 5. Estado inicial de la transacción
        TransactionStatus statusCompletado = transactionStatusRepository.findByNameStatus("COMPLETADO")
                .orElseThrow(() -> new IllegalStateException("Estado COMPLETADO no configurado."));

        // 5b. Resolver Promotion (opcional), igual que el monolito
        Promotion promotion = null;
        if (request.getIdPromotion() != null) {
            promotion = promotionRepository.findById(request.getIdPromotion()).orElse(null);
        }

        String qrCodeData = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        // 6. Guardar la transacción. Confiamos en los totales que manda el frontend,
        //    igual que hacía el monolito (no se recalculan).
        SaleTransaction transaction = SaleTransaction.builder()
                .idUser(user.getIdUser())
                .promotion(promotion)
                .transactionStatus(statusCompletado)
                .subtotal(request.getSubtotal())
                .discountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO)
                .totalAmount(request.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .paymentDate(LocalDateTime.now())
                .qrCodeData(qrCodeData)
                .build();
        transaction = saleTransactionRepository.save(transaction);

        // 7. Guardar un SaleTicketDetail por cada asiento
        for (int i = 0; i < seatIds.size(); i++) {
            saleTicketDetailRepository.save(SaleTicketDetail.builder()
                    .saleTransaction(transaction)
                    .idShowtime(request.getIdShowtime())
                    .idSeat(seatIds.get(i))
                    .ticketPrice(preciosPorAsiento.get(i))
                    .isUsed(false)
                    .build());
        }

        // 8. Snacks: validar y descontar stock vía confiteria-service.
        //    idSnackVenueStock (el registro) es distinto de idSnack (el producto) -
        //    hay que buscarlo primero filtrando por snack + sede. La sede es la de la
        //    función; sin función (solo confitería), la del usuario.
        Integer idVenueVenta = showtime != null ? showtime.getIdVenue() : user.getIdVenue();
        if (request.getSnacks() != null) {
            for (var linea : request.getSnacks()) {

                List<SnackStockDTO> registros = confiteriaClient.findBySnack(linea.getIdSnack());
                SnackStockDTO stock = registros.stream()
                        .filter(r -> Objects.equals(r.getIdVenue(), idVenueVenta))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                "El snack " + linea.getIdSnack() + " no está disponible en esta sede."));

                if (stock.getStock() < linea.getCantidad()) {
                    throw new IllegalStateException("Stock insuficiente para el snack " + linea.getIdSnack());
                }

                stock.setStock(stock.getStock() - linea.getCantidad());
                confiteriaClient.updateStock(stock.getIdSnackVenueStock(), stock);

                saleSnackDetailRepository.save(SaleSnackDetail.builder()
                        .saleTransaction(transaction)
                        .idSnack(linea.getIdSnack())
                        .quantity(linea.getCantidad())
                        .unitPrice(linea.getUnitPrice())
                        .isDelivered(false)
                        .build());
            }
        }

        // 9. Reducir availableSeats en cartelera-service (solo si hay showtime), igual que el monolito.
        if (showtime != null && !seatIds.isEmpty()) {
            carteleraClient.decreaseAvailableSeats(request.getIdShowtime(), seatIds.size());
        }

        return new SaleTransactionResponseDTO(transaction.getIdTransaction(), qrCodeData, "Reserva confirmada exitosamente.");
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