package com.cinemax.cartelera.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.cinemax.cartelera.dto.ShowtimeRequestDTO;
import com.cinemax.cartelera.dto.ShowtimeResponseDTO;
import com.cinemax.cartelera.dto.ShowtimeSummaryDTO;
import com.cinemax.cartelera.dto.TicketFareDTO;
import com.cinemax.cartelera.service.ShowtimeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/showtimes")
public class ShowtimeController {

    @Autowired
    private ShowtimeService showtimeService;

    @GetMapping
    public ResponseEntity<List<ShowtimeResponseDTO>> findAll(
            @RequestParam(required = false) String idMovie,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(showtimeService.findAll(idMovie, date));
    }

    @GetMapping("/by-venue")
    public ResponseEntity<List<ShowtimeResponseDTO>> findByVenue(
            @RequestParam Integer idVenue,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(showtimeService.findByVenueAndDate(idVenue, date));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<ShowtimeSummaryDTO> getSummary(@PathVariable String id) {
        return ResponseEntity.ok(showtimeService.getSummary(id));
    }

    @GetMapping("/{id}/fares")
    public ResponseEntity<List<TicketFareDTO>> getFares(@PathVariable String id) {
        return ResponseEntity.ok(showtimeService.getFares(id));
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowtimeResponseDTO>> findByMovieId(@PathVariable String movieId) {
        return ResponseEntity.ok(showtimeService.findByMovieId(movieId));
    }

    @GetMapping("/date/{showDate}")
    public ResponseEntity<List<ShowtimeResponseDTO>> findByShowDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDate) {
        return ResponseEntity.ok(showtimeService.findByShowDate(showDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeResponseDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(showtimeService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_SHOWTIMES')")
    public ResponseEntity<ShowtimeResponseDTO> create(@Valid @RequestBody ShowtimeRequestDTO dto) {
        ShowtimeResponseDTO created = showtimeService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_SHOWTIMES')")
    public ResponseEntity<ShowtimeResponseDTO> update(@PathVariable String id, @Valid @RequestBody ShowtimeRequestDTO dto) {
        return ResponseEntity.ok(showtimeService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_SHOWTIMES')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        showtimeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}