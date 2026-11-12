package com.cinemax.facturacion.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_ticket_detail", uniqueConstraints = {
        @UniqueConstraint(name = "uq_ticket_seat_showtime", columnNames = {"id_showtime", "id_seat"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleTicketDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ticket")
    private Integer idTicket;

    @ManyToOne
    @JoinColumn(name = "id_transaction", nullable = false)
    private SaleTransaction saleTransaction;

    // Dueño real: cartelera-service (MongoDB -> ObjectId como String).
    @Column(name = "id_showtime", nullable = false, length = 24)
    private String idShowtime;

    // Dueño real: sucursales-service.
    @Column(name = "id_seat", nullable = false)
    private Integer idSeat;

    @Column(name = "ticket_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal ticketPrice;

    @Builder.Default
    @Column(name = "is_used")
    private Boolean isUsed = false;

    // ADULTO, NINO, ADULTO_MAYOR o DISCAPACITADO.
    @Column(name = "fare_category", length = 20)
    private String fareCategory;

    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
}