import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface TicketLineDTO {
  categoryCode: string;
  cantidad: number;
  precioUnitario: number;
}

export interface SnackLineDTO {
  idSnack: number;
  cantidad: number;
  unitPrice: number;
}

export interface SaleTransactionRequestDTO {
  idShowtime: string;
  asientosIds: number[];
  tickets: TicketLineDTO[];
  snacks: SnackLineDTO[];
  subtotal: number;
  discountAmount: number;
  totalAmount: number;
  paymentMethod: 'TARJETA' | 'YAPE';
  idPromotion: number | null;
}

export interface SaleTransactionResponseDTO {
  idTransaction: number;
  qrCodeData: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class SaleTransactionService {
  private readonly apiUrl = `${environment.apiUrl}/sale-transactions`;

  constructor(private http: HttpClient) {}

  createSaleTransaction(request: SaleTransactionRequestDTO): Observable<SaleTransactionResponseDTO> {
    return this.http.post<SaleTransactionResponseDTO>(this.apiUrl, request);
  }
}
