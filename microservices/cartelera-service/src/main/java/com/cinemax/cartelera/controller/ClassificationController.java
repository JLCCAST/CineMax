package com.cinemax.cartelera.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.cinemax.cartelera.dto.ClassificationRequestDTO;
import com.cinemax.cartelera.dto.ClassificationResponseDTO;
import com.cinemax.cartelera.service.ClassificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/classifications")
public class ClassificationController {

    @Autowired
    private ClassificationService classificationService;

    @GetMapping
    public ResponseEntity<List<ClassificationResponseDTO>> findAll() {
        return ResponseEntity.ok(classificationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassificationResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(classificationService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_MOVIES')")
    public ResponseEntity<ClassificationResponseDTO> create(@Valid @RequestBody ClassificationRequestDTO dto) {
        ClassificationResponseDTO created = classificationService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_MOVIES')")
    public ResponseEntity<ClassificationResponseDTO> update(@PathVariable String id, @Valid @RequestBody ClassificationRequestDTO dto) {
        return ResponseEntity.ok(classificationService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_MOVIES')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        classificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}