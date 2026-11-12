package com.cinemax.facturacion.repository;

import com.cinemax.facturacion.model.entity.PromotionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromotionTypeRepository extends JpaRepository<PromotionType, Integer> {
    Optional<PromotionType> findByNamePromotionType(String namePromotionType);
}
