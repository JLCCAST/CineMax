package com.cinemax.facturacion.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "promotion")
public class Promotion {

    @Id
    @Column(name = "id_promotion")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPromotion;

    @ManyToOne
    @JoinColumn(name = "id_promotion_type", nullable = false)
    private PromotionType promotionType;

    @Column(name = "name_promotion", nullable = false, length = 100)
    private String namePromotion;

    @Column(name = "promotion_code", unique = true, length = 30)
    private String promotionCode;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    @Min(value = 0, message = "El porcentaje de descuento no puede ser negativo")
    private BigDecimal discountPercentage;

    @Column(name = "discount_fixed_amount", precision = 10, scale = 2)
    @Min(value = 0, message = "El monto de descuento fijo no puede ser negativo")
    private BigDecimal discountFixedAmount;

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Builder.Default
    @Column(name = "minimum_purchase_amount", precision = 10, scale = 2)
    private BigDecimal minimumPurchaseAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "minimum_ticket_quantity")
    private Integer minimumTicketQuantity = 1;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "day_of_week", length = 20)
    private String dayOfWeek;

    @Builder.Default
    @Column(name = "exclude_premiere_movies")
    private Boolean excludePremiereMovies = false;

    @Builder.Default
    @Column(name = "status", length = 20)
    @Pattern(regexp = "Activo|Inactivo", message = "El estado debe ser 'Activo' o 'Inactivo'")
    private String status = "Activo";

    @Builder.Default
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "image_url_promotion", length = 250)
    private String imageUrlPromotion;

    // ---- Condiciones y alcance de las promociones automáticas ----

    @Builder.Default
    @Column(name = "applies_to", nullable = false, length = 20)
    @Pattern(regexp = "ENTRADAS|CONFITERIA|TODO", message = "El alcance debe ser ENTRADAS, CONFITERIA o TODO")
    private String appliesTo = "TODO";

    @Column(name = "start_time_from")
    private LocalTime startTimeFrom;

    // La función debe empezar antes de esta hora.
    @Column(name = "start_time_to")
    private LocalTime startTimeTo;

    // Categoría de confitería (otra base, sin clave foránea).
    @Column(name = "required_snack_category_id")
    private Integer requiredSnackCategoryId;

    @Column(name = "customer_condition", length = 20)
    @Pattern(regexp = "CUMPLEANOS", message = "La única condición del cliente es CUMPLEANOS")
    private String customerCondition;

    // Usos por cliente dentro del año calendario.
    @Column(name = "max_uses_per_user")
    private Integer maxUsesPerUser;

    @Builder.Default
    @Column(name = "priority", nullable = false)
    private Integer priority = 0;
}
