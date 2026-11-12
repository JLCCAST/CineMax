package com.cinemax.confiteria.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cinemax.confiteria.entity.SnackVenueStock;

public interface SnackVenueStockRepository extends JpaRepository<SnackVenueStock, Integer> {

    List<SnackVenueStock> findByIdVenue(Integer idVenue);

    List<SnackVenueStock> findBySnack_IdSnack(Integer idSnack);

    List<SnackVenueStock> findByIdVenueAndSnack_IdSnack(Integer idVenue, Integer idSnack);

    List<SnackVenueStock> findByIdVenueAndStatusAndStockGreaterThan(Integer idVenue, String status, Integer stock);

    long countBySnack_IdSnack(Integer idSnack);

    // Resta solo si el snack está activo en la sede y alcanza el stock. Es una
    // sola sentencia: dos compras simultáneas no pueden dejarlo en negativo.
    @Modifying
    @Query(value = "UPDATE snack_venue_stock SET stock = stock - :cantidad "
            + "WHERE id_venue = :idVenue AND id_snack = :idSnack AND status = 'Activo' AND stock >= :cantidad",
            nativeQuery = true)
    int descontar(@Param("idVenue") Integer idVenue, @Param("idSnack") Integer idSnack,
                  @Param("cantidad") Integer cantidad);

    @Modifying
    @Query(value = "UPDATE snack_venue_stock SET stock = stock + :cantidad "
            + "WHERE id_venue = :idVenue AND id_snack = :idSnack",
            nativeQuery = true)
    int reponer(@Param("idVenue") Integer idVenue, @Param("idSnack") Integer idSnack,
                @Param("cantidad") Integer cantidad);
}
