package com.cinemax.facturacion.promociones;

import com.cinemax.facturacion.dto.request.PromotionAdminDTO;
import com.cinemax.facturacion.model.entity.Promotion;
import com.cinemax.facturacion.model.entity.PromotionType;
import com.cinemax.facturacion.repository.PromotionRepository;
import com.cinemax.facturacion.repository.PromotionTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Gestión de promociones desde el admin. No se borran: las ventas las referencian.
@Service
@RequiredArgsConstructor
public class PromotionAdminService {

    private final PromotionRepository promotionRepository;
    private final PromotionTypeRepository promotionTypeRepository;

    public List<PromotionAdminDTO> listar() {
        return promotionRepository.findAllByOrderByIdPromotionAsc().stream().map(PromotionAdminDTO::from).toList();
    }

    public PromotionAdminDTO obtener(Integer id) {
        return PromotionAdminDTO.from(buscar(id));
    }

    public List<String> tipos() {
        return promotionTypeRepository.findAll().stream().map(PromotionType::getNamePromotionType).sorted().toList();
    }

    @Transactional
    public PromotionAdminDTO crear(PromotionAdminDTO dto) {
        Promotion p = Promotion.builder().build();
        aplicar(p, dto);
        p.setStatus(dto.getStatus() != null ? dto.getStatus() : "Activo");
        p.setCreatedAt(LocalDateTime.now());
        return PromotionAdminDTO.from(promotionRepository.save(p));
    }

    @Transactional
    public PromotionAdminDTO actualizar(Integer id, PromotionAdminDTO dto) {
        Promotion p = buscar(id);
        aplicar(p, dto);
        if (dto.getStatus() != null) {
            p.setStatus(dto.getStatus());
        }
        p.setUpdatedAt(LocalDateTime.now());
        return PromotionAdminDTO.from(promotionRepository.save(p));
    }

    @Transactional
    public void cambiarEstado(Integer id, String status) {
        if (!"Activo".equals(status) && !"Inactivo".equals(status)) {
            throw new IllegalStateException("El estado debe ser Activo o Inactivo.");
        }
        Promotion p = buscar(id);
        p.setStatus(status);
        p.setUpdatedAt(LocalDateTime.now());
        promotionRepository.save(p);
    }

    private Promotion buscar(Integer id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promoción no encontrada."));
    }

    private void aplicar(Promotion p, PromotionAdminDTO dto) {
        PromotionType tipo = promotionTypeRepository.findByNamePromotionType(dto.getPromotionType())
                .orElseThrow(() -> new IllegalStateException("Tipo de beneficio no válido: " + dto.getPromotionType()));
        String codigo = dto.getPromotionCode() == null || dto.getPromotionCode().isBlank()
                ? null : dto.getPromotionCode().trim().toUpperCase();
        validar(dto, tipo, codigo, p.getIdPromotion());

        p.setNamePromotion(dto.getNamePromotion().trim());
        p.setPromotionType(tipo);
        p.setPromotionCode(codigo);
        p.setDiscountPercentage(dto.getDiscountPercentage());
        p.setDiscountFixedAmount(dto.getDiscountFixedAmount());
        p.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        p.setMinimumPurchaseAmount(dto.getMinimumPurchaseAmount() != null ? dto.getMinimumPurchaseAmount() : BigDecimal.ZERO);
        p.setMinimumTicketQuantity(dto.getMinimumTicketQuantity() != null ? dto.getMinimumTicketQuantity() : 1);
        p.setStartDate(dto.getStartDate());
        p.setEndDate(dto.getEndDate());
        p.setDayOfWeek(dto.getDayOfWeek() == null || dto.getDayOfWeek().isBlank() ? null : dto.getDayOfWeek());
        p.setStartTimeFrom(dto.getStartTimeFrom());
        p.setStartTimeTo(dto.getStartTimeTo());
        p.setExcludePremiereMovies(Boolean.TRUE.equals(dto.getExcludePremiereMovies()));
        p.setRequiredSnackCategoryId(dto.getRequiredSnackCategoryId());
        p.setCustomerCondition(dto.getCustomerCondition());
        p.setMaxUsesPerUser(dto.getMaxUsesPerUser());
        p.setAppliesTo(dto.getAppliesTo() != null ? dto.getAppliesTo() : "TODO");
        p.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
    }

    private void validar(PromotionAdminDTO dto, PromotionType tipo, String codigo, Integer id) {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalStateException("La fecha de fin no puede ser anterior a la de inicio.");
        }
        switch (tipo.getNamePromotionType()) {
            case "PORCENTAJE" -> {
                if (dto.getDiscountPercentage() == null || dto.getDiscountPercentage().signum() <= 0) {
                    throw new IllegalStateException("Indica el porcentaje de descuento.");
                }
            }
            case "MONTO_FIJO" -> {
                if (dto.getDiscountFixedAmount() == null || dto.getDiscountFixedAmount().signum() <= 0) {
                    throw new IllegalStateException("Indica el monto fijo de descuento.");
                }
            }
            default -> { }
        }
        if (dto.getStartTimeFrom() != null && dto.getStartTimeTo() != null
                && !dto.getStartTimeFrom().isBefore(dto.getStartTimeTo())) {
            throw new IllegalStateException("La franja horaria no es válida.");
        }
        if (codigo != null && (id == null
                ? promotionRepository.existsByPromotionCodeIgnoreCase(codigo)
                : promotionRepository.existsByPromotionCodeIgnoreCaseAndIdPromotionNot(codigo, id))) {
            throw new IllegalStateException("Ya existe una promoción con el cupón " + codigo + ".");
        }
    }
}
