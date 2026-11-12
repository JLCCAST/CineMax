package com.cinemax.confiteria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinemax.confiteria.entity.SnackVenueStock;

public interface SnackVenueStockRepository extends JpaRepository<SnackVenueStock, Integer> {

    List<SnackVenueStock> findByIdVenue(Integer idVenue);

    List<SnackVenueStock> findBySnack_IdSnack(Integer idSnack);

    List<SnackVenueStock> findByIdVenueAndSnack_IdSnack(Integer idVenue, Integer idSnack);

    List<SnackVenueStock> findByIdVenueAndStatusAndStockGreaterThan(Integer idVenue, String status, Integer stock);

    long countBySnack_IdSnack(Integer idSnack);
}
