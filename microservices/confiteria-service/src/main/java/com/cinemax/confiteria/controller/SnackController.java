package com.cinemax.confiteria.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.cinemax.confiteria.dto.SnackRequestDTO;
import com.cinemax.confiteria.dto.SnackResponseDTO;
import com.cinemax.confiteria.service.SnackService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/snacks")
public class SnackController {

    @Autowired
    private SnackService snackService;

    @GetMapping
    public ResponseEntity<List<SnackResponseDTO>> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer venueId) {

        if (status != null) {
            return ResponseEntity.ok(snackService.findByStatus(status));
        }
        if (categoryId != null) {
            return ResponseEntity.ok(snackService.findByCategory(categoryId));
        }
        if (venueId != null) {
            return ResponseEntity.ok(snackService.findByVenue(venueId));
        }
        return ResponseEntity.ok(snackService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SnackResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(snackService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<SnackResponseDTO> create(@Valid @RequestBody SnackRequestDTO dto) {
        SnackResponseDTO created = snackService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<SnackResponseDTO> update(@PathVariable Integer id, @Valid @RequestBody SnackRequestDTO dto) {
        return ResponseEntity.ok(snackService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        snackService.delete(id);
        return ResponseEntity.noContent().build();
    }
}