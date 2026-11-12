import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface TicketLineDTO {
  categoryCode: string;
  cantidad: number;
}

export interface SnackLineDTO {
  idSnack: number;
  cantidad: number;
}

// Los precios los pone el servidor; totalAmount es el total que vio el cliente.
export interface SaleTransactionRequestDTO {
  idShowtime: string;
  asientosIds: number[];
  tickets: TicketLineDTO[];
  snacks: SnackLineDTO[];
  totalAmount: number;
  paymentMethod: 'TARJETA' | 'YAPE';
  promotionCode: string | null;
}

export interface SaleTransactionResponseDTO {
  idTransaction: number;
  qrCodeData: string;
  message: string;
  subtotal: number;
  discountAmount: number;
  totalAmount: number;
  namePromotion: string | null;
}

@Injectable({ providedIn: 'root' })
export class SaleTransactionService {
  private readonly apiUrl = `${environment.apiUrl}/sale-transactions`;

  constructor(private http: HttpClient) {}

  createSaleTransaction(request: SaleTransactionRequestDTO): Observable<SaleTransactionResponseDTO> {
    return this.http.post<SaleTransactionResponseDTO>(this.apiUrl, request);
  }
}
