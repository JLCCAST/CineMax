import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SeatService {
  private apiUrl = `${environment.apiUrl}/seats`;

  constructor(private http: HttpClient) {}

  // El mapa de butacas de una función lo arma facturación (sabe qué butacas se vendieron).
  getSeatsStatusByShowtime(idShowtime: string): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/availability/showtimes/${idShowtime}/seats`);
  }

  getSeatsByRoom(idRoom: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/room/${idRoom}`);
  }

  updateSeat(idSeat: number, seat: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${idSeat}`, seat);
  }
}
