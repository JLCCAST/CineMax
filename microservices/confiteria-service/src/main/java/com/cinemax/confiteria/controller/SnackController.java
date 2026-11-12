package com.cinemax.confiteria.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.cinemax.confiteria.dto.SnackRequestDTO;
import com.cinemax.confiteria.dto.SnackResponseDTO;
import com.cinemax.confiteria.service.SnackSedeService;
import com.cinemax.confiteria.service.SnackService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/snacks")
public class SnackController {

    @Autowired
    private SnackService snackService;

    @Autowired
    private SnackSedeService snackSedeService;

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

    @GetMapping("/category/{idCategory}")
    public ResponseEntity<List<SnackResponseDTO>> findByCategory(@PathVariable Integer idCategory) {
        return ResponseEntity.ok(snackService.findByCategory(idCategory));
    }

    // ---- Snacks por sede ----------------------------------------------------

    @GetMapping("/venue/{idVenue}")
    public ResponseEntity<List<SnackResponseDTO>> catalogoDeSede(@PathVariable Integer idVenue) {
        return ResponseEntity.ok(snackSedeService.catalogoDeSede(idVenue));
    }

    @GetMapping("/venue/{idVenue}/admin")
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<List<SnackResponseDTO>> catalogoAdmin(@PathVariable Integer idVenue) {
        return ResponseEntity.ok(snackSedeService.catalogoAdmin(idVenue));
    }

    @GetMapping("/{idSnack}/sedes-count")
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<Long> contarSedes(@PathVariable Integer idSnack) {
        return ResponseEntity.ok(snackSedeService.contarSedes(idSnack));
    }

    @PatchMapping("/{idSnack}/venue/{idVenue}/habilitar")
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<Void> habilitar(@PathVariable Integer idSnack, @PathVariable Integer idVenue) {
        snackSedeService.cambiarEstado(idSnack, idVenue, true);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idSnack}/venue/{idVenue}/inhabilitar")
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<Void> inhabilitar(@PathVariable Integer idSnack, @PathVariable Integer idVenue) {
        snackSedeService.cambiarEstado(idSnack, idVenue, false);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{idSnack}/venue/{idVenue}/stock")
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<Void> actualizarStock(
            @PathVariable Integer idSnack, @PathVariable Integer idVenue, @RequestParam Integer stock) {
        snackSedeService.actualizarStock(idSnack, idVenue, stock);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{idSnack}/venue/{idVenue}/agregar")
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<Void> agregarASede(
            @PathVariable Integer idSnack, @PathVariable Integer idVenue, @RequestParam Integer stock) {
        snackSedeService.agregarASede(idSnack, idVenue, stock);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{idSnack}/venue/todas/agregar")
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<Void> agregarATodas(@PathVariable Integer idSnack, @RequestParam Integer stock) {
        snackSedeService.agregarATodas(idSnack, stock);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{idSnack}/venue/{idVenue}")
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<Void> quitarDeSede(@PathVariable Integer idSnack, @PathVariable Integer idVenue) {
        snackSedeService.quitarDeSede(idSnack, idVenue);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{idSnack}/venue/todas")
    @PreAuthorize("hasAuthority('MANAGE_CONFITERIA')")
    public ResponseEntity<Void> eliminarDeTodo(@PathVariable Integer idSnack) {
        snackSedeService.eliminarDeTodo(idSnack);
        return ResponseEntity.noContent().build();
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