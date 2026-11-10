package com.cinemax.cartelera.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.cinemax.cartelera.dto.GenreRequestDTO;
import com.cinemax.cartelera.dto.GenreResponseDTO;
import com.cinemax.cartelera.service.GenreService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/genres")
public class GenreController {

    @Autowired
    private GenreService genreService;

    @GetMapping
    public ResponseEntity<List<GenreResponseDTO>> findAll() {
        return ResponseEntity.ok(genreService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenreResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(genreService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_MOVIES')")
    public ResponseEntity<GenreResponseDTO> create(@Valid @RequestBody GenreRequestDTO dto) {
        GenreResponseDTO created = genreService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_MOVIES')")
    public ResponseEntity<GenreResponseDTO> update(@PathVariable String id, @Valid @RequestBody GenreRequestDTO dto) {
        return ResponseEntity.ok(genreService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_MOVIES')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        genreService.delete(id);
        return ResponseEntity.noContent().build();
    }
}