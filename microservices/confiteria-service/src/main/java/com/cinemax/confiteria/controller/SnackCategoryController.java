package com.cinemax.confiteria.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.cinemax.confiteria.dto.SnackCategoryRequestDTO;
import com.cinemax.confiteria.dto.SnackCategoryResponseDTO;
import com.cinemax.confiteria.service.SnackCategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/snack-categories")
public class SnackCategoryController {

    @Autowired
    private SnackCategoryService snackCategoryService;

    @GetMapping
    public ResponseEntity<List<SnackCategoryResponseDTO>> findAll() {
        return ResponseEntity.ok(snackCategoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SnackCategoryResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(snackCategoryService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<SnackCategoryResponseDTO> create(@Valid @RequestBody SnackCategoryRequestDTO dto) {
        SnackCategoryResponseDTO created = snackCategoryService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<SnackCategoryResponseDTO> update(@PathVariable Integer id, @Valid @RequestBody SnackCategoryRequestDTO dto) {
        return ResponseEntity.ok(snackCategoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        snackCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}