package com.cinemax.confiteria.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cinemax.confiteria.dto.SnackCategoryResponseDTO;
import com.cinemax.confiteria.dto.SnackRequestDTO;
import com.cinemax.confiteria.dto.SnackResponseDTO;
import com.cinemax.confiteria.entity.Snack;
import com.cinemax.confiteria.entity.SnackCategory;
import com.cinemax.confiteria.repository.SnackCategoryRepository;
import com.cinemax.confiteria.repository.SnackRepository;

@Service
public class SnackService {

    @Autowired
    private SnackRepository snackRepository;

    @Autowired
    private SnackCategoryRepository snackCategoryRepository;

    public List<SnackResponseDTO> findAll() {
        return snackRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<SnackResponseDTO> findByStatus(String status) {
        return snackRepository.findByStatus(status)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<SnackResponseDTO> findByCategory(Integer categoryId) {
        return snackRepository.findBySnackCategory_IdSnackCategory(categoryId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<SnackResponseDTO> findByVenue(Integer idVenue) {
        return snackRepository.findByIdVenue(idVenue)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public SnackResponseDTO findById(Integer id) {
        Snack snack = snackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Snack no encontrado con id: " + id));
        return toResponseDTO(snack);
    }

    public SnackResponseDTO create(SnackRequestDTO dto) {
        if (snackRepository.existsByNameSnack(dto.getNameSnack())) {
            throw new RuntimeException("Ya existe un snack con ese nombre");
        }

        SnackCategory category = snackCategoryRepository.findById(dto.getSnackCategoryId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + dto.getSnackCategoryId()));

        Snack snack = Snack.builder()
                .idVenue(dto.getIdVenue())
                .snackCategory(category)
                .nameSnack(dto.getNameSnack())
                .descriptionSnack(dto.getDescriptionSnack())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .imageUrlSnack(dto.getImageUrlSnack())
                .status(dto.getStatus() != null ? dto.getStatus() : "Activo")
                .build();

        return toResponseDTO(snackRepository.save(snack));
    }

    public SnackResponseDTO update(Integer id, SnackRequestDTO dto) {
        Snack snack = snackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Snack no encontrado con id: " + id));

        if (dto.getSnackCategoryId() != null) {
            SnackCategory category = snackCategoryRepository.findById(dto.getSnackCategoryId())
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada con id: " + dto.getSnackCategoryId()));
            snack.setSnackCategory(category);
        }

        snack.setIdVenue(dto.getIdVenue());
        snack.setNameSnack(dto.getNameSnack());
        snack.setDescriptionSnack(dto.getDescriptionSnack());
        snack.setPrice(dto.getPrice());
        snack.setStock(dto.getStock());
        snack.setImageUrlSnack(dto.getImageUrlSnack());

        if (dto.getStatus() != null) {
            snack.setStatus(dto.getStatus());
        }

        return toResponseDTO(snackRepository.save(snack));
    }

    public void delete(Integer id) {
        if (!snackRepository.existsById(id)) {
            throw new RuntimeException("Snack no encontrado con id: " + id);
        }
        snackRepository.deleteById(id);
    }

    public SnackResponseDTO toResponseDTO(Snack snack) {
        SnackCategoryResponseDTO categoryDTO = SnackCategoryResponseDTO.builder()
                .idSnackCategory(snack.getSnackCategory().getIdSnackCategory())
                .nameCategory(snack.getSnackCategory().getNameCategory())
                .build();

        return SnackResponseDTO.builder()
                .idSnack(snack.getIdSnack())
                .idVenue(snack.getIdVenue())
                .snackCategory(categoryDTO)
                .idSnackCategory(categoryDTO.getIdSnackCategory())
                .nameCategory(categoryDTO.getNameCategory())
                .nameSnack(snack.getNameSnack())
                .descriptionSnack(snack.getDescriptionSnack())
                .price(snack.getPrice())
                .stock(snack.getStock())
                .imageUrlSnack(snack.getImageUrlSnack())
                .status(snack.getStatus())
                .createdAt(snack.getCreatedAt())
                .build();
    }
}