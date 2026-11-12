package com.cinemax.facturacion.repository;

import com.cinemax.facturacion.model.entity.SaleSnackDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SaleSnackDetailRepository extends JpaRepository<SaleSnackDetail, Integer> {
    List<SaleSnackDetail> findBySaleTransaction_IdTransaction(Integer idTransaction);

    List<SaleSnackDetail> findBySaleTransaction_IdTransactionIn(Collection<Integer> idTransactions);
}