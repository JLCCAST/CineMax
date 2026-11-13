package com.cinemax.facturacion.dto.request;

import com.cinemax.facturacion.model.entity.Promotion;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

// Promoción tal como la gestiona el admin (entrada y salida del CRUD).
@Data
public class PromotionAdminDTO {

    private Integer idPromotion;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String namePromotion;

    @NotBlank(message = "El tipo de beneficio es obligatorio")
    private String promotionType;

    @Size(max = 30)
    private String promotionCode;

    @DecimalMin(value = "0", message = "El porcentaje no puede ser negativo")
    @DecimalMax(value = "100", message = "El porcentaje no puede pasar de 100")
    private BigDecimal discountPercentage;

    @DecimalMin(value = "0", message = "El monto fijo no puede ser negativo")
    private BigDecimal discountFixedAmount;

    @DecimalMin(value = "0", message = "El tope no puede ser negativo")
    private BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0", message = "La compra mínima no puede ser negativa")
    private BigDecimal minimumPurchaseAmount;

    @Min(value = 1, message = "Debe pedir al menos una entrada")
    private Integer minimumTicketQuantity;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate endDate;

    @Pattern(regexp = "[1-7](,[1-7])*", message = "Los días van como números ISO separados por coma (1 = lunes)")
    private String dayOfWeek;

    private LocalTime startTimeFrom;
    private LocalTime startTimeTo;
    private Boolean excludePremiereMovies;
    private Integer requiredSnackCategoryId;

    @Pattern(regexp = "CUMPLEANOS", message = "La única condición del cliente es CUMPLEANOS")
    private String customerCondition;

    @Min(value = 1, message = "El límite de usos debe ser al menos 1")
    private Integer maxUsesPerUser;

    @Pattern(regexp = "ENTRADAS|CONFITERIA|TODO", message = "El alcance debe ser ENTRADAS, CONFITERIA o TODO")
    private String appliesTo;

    private Integer priority;

    @Pattern(regexp = "Activo|Inactivo", message = "El estado debe ser Activo o Inactivo")
    private String status;

    @Size(max = 300, message = "La descripción admite hasta 300 caracteres")
    private String description;

    @Size(max = 250, message = "La ruta de la imagen admite hasta 250 caracteres")
    private String imageUrlPromotion;

    public static PromotionAdminDTO from(Promotion p) {
        PromotionAdminDTO dto = new PromotionAdminDTO();
        dto.setIdPromotion(p.getIdPromotion());
        dto.setNamePromotion(p.getNamePromotion());
        dto.setPromotionType(p.getPromotionType().getNamePromotionType());
        dto.setPromotionCode(p.getPromotionCode());
        dto.setDiscountPercentage(p.getDiscountPercentage());
        dto.setDiscountFixedAmount(p.getDiscountFixedAmount());
        dto.setMaxDiscountAmount(p.getMaxDiscountAmount());
        dto.setMinimumPurchaseAmount(p.getMinimumPurchaseAmount());
        dto.setMinimumTicketQuantity(p.getMinimumTicketQuantity());
        dto.setStartDate(p.getStartDate());
        dto.setEndDate(p.getEndDate());
        dto.setDayOfWeek(p.getDayOfWeek());
        dto.setStartTimeFrom(p.getStartTimeFrom());
        dto.setStartTimeTo(p.getStartTimeTo());
        dto.setExcludePremiereMovies(p.getExcludePremiereMovies());
        dto.setRequiredSnackCategoryId(p.getRequiredSnackCategoryId());
        dto.setCustomerCondition(p.getCustomerCondition());
        dto.setMaxUsesPerUser(p.getMaxUsesPerUser());
        dto.setAppliesTo(p.getAppliesTo());
        dto.setPriority(p.getPriority());
        dto.setStatus(p.getStatus());
        dto.setDescription(p.getDescription());
        dto.setImageUrlPromotion(p.getImageUrlPromotion());
        return dto;
    }
}
