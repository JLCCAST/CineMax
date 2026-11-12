package com.cinemax.facturacion.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "sale_snack_detail")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleSnackDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detail")
    private Integer idDetail;

    @ManyToOne
    @JoinColumn(name = "id_transaction", nullable = false)
    private SaleTransaction saleTransaction;

    // Dueño real: confiteria-service.
    @Column(name = "id_snack", nullable = false)
    private Integer idSnack;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Builder.Default
    @Column(name = "is_delivered")
    private Boolean isDelivered = false;

    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
}