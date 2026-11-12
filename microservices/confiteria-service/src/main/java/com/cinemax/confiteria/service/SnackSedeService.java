package com.cinemax.confiteria.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinemax.confiteria.client.SucursalesClient;
import com.cinemax.confiteria.dto.SnackResponseDTO;
import com.cinemax.confiteria.dto.external.VenueDTO;
import com.cinemax.confiteria.entity.Snack;
import com.cinemax.confiteria.entity.SnackVenueStock;
import com.cinemax.confiteria.repository.SnackRepository;
import com.cinemax.confiteria.repository.SnackVenueStockRepository;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

// Snacks por sede: qué vende cada sede, con su stock y su estado.
@Service
@RequiredArgsConstructor
public class SnackSedeService {

    private static final String ACTIVO = "Activo";
    private static final String INACTIVO = "Inactivo";

    private final SnackRepository snackRepository;
    private final SnackVenueStockRepository stockRepository;
    private final SnackService snackService;
    private final SucursalesClient sucursalesClient;

    // Lo que puede comprar un cliente en la sede: activos y con stock.
    public List<SnackResponseDTO> catalogoDeSede(Integer idVenue) {
        return stockRepository.findByIdVenueAndStatusAndStockGreaterThan(idVenue, ACTIVO, 0).stream()
                .filter(registro -> ACTIVO.equals(registro.getSnack().getStatus()))
                .map(this::toDTO)
                .toList();
    }

    public List<SnackResponseDTO> catalogoAdmin(Integer idVenue) {
        return stockRepository.findByIdVenue(idVenue).stream().map(this::toDTO).toList();
    }

    public long contarSedes(Integer idSnack) {
        return stockRepository.countBySnack_IdSnack(idSnack);
    }

    public void cambiarEstado(Integer idSnack, Integer idVenue, boolean activo) {
        SnackVenueStock registro = buscarRegistro(idSnack, idVenue);
        registro.setStatus(activo ? ACTIVO : INACTIVO);
        stockRepository.save(registro);
    }

    public void actualizarStock(Integer idSnack, Integer idVenue, Integer stock) {
        validarStock(stock);
        SnackVenueStock registro = buscarRegistro(idSnack, idVenue);
        registro.setStock(stock);
        stockRepository.save(registro);
    }

    public void agregarASede(Integer idSnack, Integer idVenue, Integer stock) {
        validarStock(stock);
        Snack snack = buscarSnack(idSnack);
        if (sedes().stream().noneMatch(sede -> sede.getIdVenue().equals(idVenue))) {
            throw new RuntimeException("Sede no encontrada con id: " + idVenue);
        }
        if (!stockRepository.findByIdVenueAndSnack_IdSnack(idVenue, idSnack).isEmpty()) {
            throw new RuntimeException("El snack ya está en esta sede");
        }
        stockRepository.save(nuevoRegistro(snack, idVenue, stock));
    }

    @Transactional
    public void agregarATodas(Integer idSnack, Integer stock) {
        validarStock(stock);
        Snack snack = buscarSnack(idSnack);
        Set<Integer> yaLoTienen = stockRepository.findBySnack_IdSnack(idSnack).stream()
                .map(SnackVenueStock::getIdVenue)
                .collect(Collectors.toSet());
        for (VenueDTO sede : sedes()) {
            if (!yaLoTienen.contains(sede.getIdVenue())) {
                stockRepository.save(nuevoRegistro(snack, sede.getIdVenue(), stock));
            }
        }
    }

    public void quitarDeSede(Integer idSnack, Integer idVenue) {
        stockRepository.delete(buscarRegistro(idSnack, idVenue));
    }

    @Transactional
    public void eliminarDeTodo(Integer idSnack) {
        Snack snack = buscarSnack(idSnack);
        stockRepository.deleteAll(stockRepository.findBySnack_IdSnack(idSnack));
        snackRepository.delete(snack);
    }

    private SnackVenueStock buscarRegistro(Integer idSnack, Integer idVenue) {
        return stockRepository.findByIdVenueAndSnack_IdSnack(idVenue, idSnack).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Snack no encontrado en esta sede"));
    }

    private Snack buscarSnack(Integer idSnack) {
        return snackRepository.findById(idSnack)
                .orElseThrow(() -> new RuntimeException("Snack no encontrado con id: " + idSnack));
    }

    private List<VenueDTO> sedes() {
        try {
            return sucursalesClient.getVenues();
        } catch (FeignException e) {
            throw new RuntimeException("No se pudo consultar las sedes");
        }
    }

    private static void validarStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new RuntimeException("El stock no puede ser negativo");
        }
    }

    private static SnackVenueStock nuevoRegistro(Snack snack, Integer idVenue, Integer stock) {
        SnackVenueStock registro = new SnackVenueStock();
        registro.setSnack(snack);
        registro.setIdVenue(idVenue);
        registro.setStock(stock);
        registro.setStatus(ACTIVO);
        return registro;
    }

    private SnackResponseDTO toDTO(SnackVenueStock registro) {
        SnackResponseDTO dto = snackService.toResponseDTO(registro.getSnack());
        dto.setIdVenue(registro.getIdVenue());
        dto.setStock(registro.getStock());
        dto.setStatus(registro.getStatus());
        return dto;
    }
}
