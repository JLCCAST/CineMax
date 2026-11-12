package com.cinemax.facturacion.dto.response;

import com.cinemax.facturacion.promociones.CotizacionService.Cotizacion;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CotizacionResponseDTO {
    private String idShowtime;
    private List<Entrada> tickets;
    private List<Snack> snacks;
    private Integer idPromotion;
    private String namePromotion;
    // Cupón de la promoción aplicada, si la promoción era de cupón.
    private String promotionCode;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    public record Entrada(String categoryCode, String categoryName, BigDecimal price, BigDecimal discountAmount) {}

    public record Snack(Integer idSnack, String nameSnack, BigDecimal unitPrice, int quantity, BigDecimal discountAmount) {}

    public static CotizacionResponseDTO from(Cotizacion c) {
        return CotizacionResponseDTO.builder()
                .idShowtime(c.contexto().getIdShowtime())
                .tickets(c.entradas().stream()
                        .map(e -> new Entrada(e.categoryCode(), e.categoryName(), e.price(), e.discount()))
                        .toList())
                .snacks(c.snacks().stream()
                        .map(s -> new Snack(s.idSnack(), s.nameSnack(), s.unitPrice(), s.quantity(), s.discount()))
                        .toList())
                .idPromotion(c.promocion() != null ? c.promocion().getIdPromotion() : null)
                .namePromotion(c.promocion() != null ? c.promocion().getNamePromotion() : null)
                .promotionCode(c.promocion() != null ? c.promocion().getPromotionCode() : null)
                .subtotal(c.subtotal())
                .discountAmount(c.descuento())
                .totalAmount(c.total())
                .build();
    }
}
