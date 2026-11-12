package com.cinemax.facturacion.repository;

import com.cinemax.facturacion.model.entity.SaleTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SaleTransactionRepository extends JpaRepository<SaleTransaction, Integer> {
    List<SaleTransaction> findByIdUserOrderByPaymentDateDesc(Integer idUser);

    // Compras completadas de un cliente con una promoción, en un periodo.
    @Query("""
            select count(t) from SaleTransaction t
            where t.idUser = :idUser
              and t.promotion.idPromotion = :idPromotion
              and t.transactionStatus.nameStatus = 'COMPLETADO'
              and t.createdAt >= :desde and t.createdAt < :hasta""")
    long contarUsos(@Param("idUser") Integer idUser, @Param("idPromotion") Integer idPromotion,
                    @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
