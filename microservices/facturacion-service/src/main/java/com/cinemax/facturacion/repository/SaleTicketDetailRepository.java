package com.cinemax.facturacion.repository;

import com.cinemax.facturacion.model.entity.SaleTicketDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SaleTicketDetailRepository extends JpaRepository<SaleTicketDetail, Integer> {
    List<SaleTicketDetail> findBySaleTransaction_IdTransaction(Integer idTransaction);

    List<SaleTicketDetail> findByIdShowtimeAndIdSeatIn(String idShowtime, Collection<Integer> idSeats);

    @Query("select t.idSeat from SaleTicketDetail t where t.idShowtime = :idShowtime")
    List<Integer> findIdSeatsByIdShowtime(@Param("idShowtime") String idShowtime);
}
