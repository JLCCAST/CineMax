package com.cinemax.confiteria.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinemax.confiteria.dto.SnackResponseDTO;
import com.cinemax.confiteria.repository.SnackRepository;
import com.cinemax.confiteria.service.SnackService;

import lombok.RequiredArgsConstructor;

/**
 * Operaciones que solo usan otros servicios. El gateway no enruta /internal/**
 * y ServiceAccessFilter exige el token de servicio.
 */
@RestController
@RequestMapping("/internal/snacks")
@RequiredArgsConstructor
public class InternalSnackController {

    private final SnackRepository snackRepository;
    private final SnackService snackService;

    // Usado por facturacion-service para cobrar con el precio y la categoría vigentes.
    @GetMapping
    public ResponseEntity<List<SnackResponseDTO>> findByIds(@RequestParam List<Integer> ids) {
        return ResponseEntity.ok(snackRepository.findAllById(ids).stream()
                .map(snackService::toResponseDTO)
                .toList());
    }
}
