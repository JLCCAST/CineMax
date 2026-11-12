import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface Promotion {
  idPromotion?: number;
  namePromotion: string;
  promotionType: string;
  promotionCode: string | null;
  discountPercentage: number | null;
  discountFixedAmount: number | null;
  maxDiscountAmount: number | null;
  minimumPurchaseAmount: number | null;
  minimumTicketQuantity: number | null;
  startDate: string;
  endDate: string;
  dayOfWeek: string | null;
  startTimeFrom: string | null;
  startTimeTo: string | null;
  excludePremiereMovies: boolean;
  requiredSnackCategoryId: number | null;
  customerCondition: string | null;
  maxUsesPerUser: number | null;
  appliesTo: 'ENTRADAS' | 'CONFITERIA' | 'TODO';
  priority: number;
  status: 'Activo' | 'Inactivo';
}

@Injectable({ providedIn: 'root' })
export class PromotionService {
  private readonly apiUrl = `${environment.apiUrl}/promotions`;

  constructor(private http: HttpClient) {}

  list(): Observable<Promotion[]> {
    return this.http.get<Promotion[]>(this.apiUrl);
  }

  types(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/types`);
  }

  create(promotion: Promotion): Observable<Promotion> {
    return this.http.post<Promotion>(this.apiUrl, promotion);
  }

  update(id: number, promotion: Promotion): Observable<Promotion> {
    return this.http.put<Promotion>(`${this.apiUrl}/${id}`, promotion);
  }

  setStatus(id: number, status: 'Activo' | 'Inactivo'): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/estado`, null, { params: new HttpParams().set('status', status) });
  }
}
