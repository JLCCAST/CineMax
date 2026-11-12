package com.cinemax.facturacion.repository;

import com.cinemax.facturacion.model.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {
    List<Promotion> findByStatus(String status);

    List<Promotion> findAllByOrderByIdPromotionAsc();

    boolean existsByPromotionCodeIgnoreCase(String promotionCode);

    boolean existsByPromotionCodeIgnoreCaseAndIdPromotionNot(String promotionCode, Integer idPromotion);
}
