package com.cinemax.facturacion.controller;

import com.cinemax.facturacion.client.UsuariosClient;
import com.cinemax.facturacion.dto.external.UserDTO;
import com.cinemax.facturacion.dto.request.CotizacionRequestDTO;
import com.cinemax.facturacion.dto.request.PromotionAdminDTO;
import com.cinemax.facturacion.dto.response.CotizacionResponseDTO;
import com.cinemax.facturacion.promociones.CotizacionService;
import com.cinemax.facturacion.promociones.CotizacionService.EntradaPedida;
import com.cinemax.facturacion.promociones.CotizacionService.SnackPedido;
import com.cinemax.facturacion.promociones.PromotionAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final CotizacionService cotizacionService;
    private final PromotionAdminService promotionAdminService;
    private final UsuariosClient usuariosClient;

    // Vista previa del checkout: precios, promoción que aplica y total.
    // Sin sesión iniciada no se evalúan las condiciones del cliente.
    @PostMapping("/preview")
    public ResponseEntity<CotizacionResponseDTO> preview(@RequestBody CotizacionRequestDTO request) {
        List<EntradaPedida> entradas = request.getTickets() == null ? List.of() : request.getTickets().stream()
                .map(t -> new EntradaPedida(t.getCategoryCode(), t.getCantidad() == null ? 0 : t.getCantidad()))
                .toList();
        List<SnackPedido> snacks = request.getSnacks() == null ? List.of() : request.getSnacks().stream()
                .map(s -> new SnackPedido(s.getIdSnack(), s.getCantidad() == null ? 0 : s.getCantidad()))
                .toList();

        return ResponseEntity.ok(CotizacionResponseDTO.from(cotizacionService.cotizar(
                request.getIdShowtime(), entradas, snacks, request.getPromotionCode(), clienteConSesion())));
    }

    // ---- Gestión (admin) -----------------------------------------------------

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    public ResponseEntity<List<PromotionAdminDTO>> listar() {
        return ResponseEntity.ok(promotionAdminService.listar());
    }

    @GetMapping("/types")
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    public ResponseEntity<List<String>> tipos() {
        return ResponseEntity.ok(promotionAdminService.tipos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    public ResponseEntity<PromotionAdminDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(promotionAdminService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    public ResponseEntity<PromotionAdminDTO> crear(@Valid @RequestBody PromotionAdminDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionAdminService.crear(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    public ResponseEntity<PromotionAdminDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody PromotionAdminDTO dto) {
        return ResponseEntity.ok(promotionAdminService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Integer id, @RequestParam String status) {
        promotionAdminService.cambiarEstado(id, status);
        return ResponseEntity.noContent().build();
    }

    private UserDTO clienteConSesion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return usuariosClient.getProfile();
    }
}
