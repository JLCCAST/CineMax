package com.cinemax.facturacion.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "sale_transaction")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaction")
    private Integer idTransaction;

    // Dueño real: usuarios-service. Solo guardamos el ID plano.
    @Column(name = "id_user", nullable = false)
    private Integer idUser;

    // Catálogo interno propio de sales_db -> sí es relación JPA normal.
    @ManyToOne
    @JoinColumn(name = "id_transaction_status", nullable = false)
    private TransactionStatus transactionStatus;

    // Promotion también vive en sales_db (facturacion-service) -> relación JPA normal, igual que el monolito.
    @ManyToOne
    @JoinColumn(name = "id_promotion")
    private Promotion promotion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Builder.Default
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Builder.Default
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "qr_code_data", length = 250)
    private String qrCodeData;

    // Datos de la función y del lugar tal como eran al comprar (ver V6).
    @Column(name = "id_showtime", length = 24)
    private String idShowtime;

    @Column(name = "id_venue")
    private Integer idVenue;

    @Column(name = "movie_title", length = 150)
    private String movieTitle;

    @Column(name = "show_date")
    private LocalDate showDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "room_name", length = 100)
    private String roomName;

    @Column(name = "venue_name", length = 150)
    private String venueName;
}