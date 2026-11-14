package com.cinemax.facturacion.service;

import com.cinemax.facturacion.dto.request.SaleTransactionRequestDTO;
import com.cinemax.facturacion.dto.response.Pagina;
import com.cinemax.facturacion.dto.response.SaleTransactionHistoryDTO;
import com.cinemax.facturacion.dto.response.SaleTransactionResponseDTO;


public interface SaleTransactionService {
    SaleTransactionResponseDTO createSaleTransaction(SaleTransactionRequestDTO request);
    Pagina<SaleTransactionHistoryDTO> getMyPurchases(int page, int size);
}