package com.cinemax.facturacion.dto.response;

import com.cinemax.facturacion.model.entity.Promotion;

// Lo que la portada muestra de una promoción vigente: sin reglas internas ni límites de uso.
public record PromocionPublicaDTO(Integer idPromotion, String namePromotion, String description,
                                  String promotionCode, String imageUrl, String appliesTo) {

    public static PromocionPublicaDTO from(Promotion p) {
        return new PromocionPublicaDTO(p.getIdPromotion(), p.getNamePromotion(), p.getDescription(),
                p.getPromotionCode(), p.getImageUrlPromotion(), p.getAppliesTo());
    }
}
