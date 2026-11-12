import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

@Injectable({ providedIn: 'root' })
export class ShowtimeService {
  private apiUrl = `${environment.apiUrl}/showtimes`;

  constructor(private http: HttpClient) {}

  getShowtimesByMovie(idMovie: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}?idMovie=${idMovie}`);
  }

  getShowtimesByVenue(idVenue: number, date: string): Observable<any[]> {
    const params = new HttpParams().set('idVenue', idVenue).set('date', date);
    return this.http.get<any[]>(`${this.apiUrl}/by-venue`, { params });
  }

  getShowtimeSummary(idShowtime: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/${idShowtime}/summary`);
  }

  getTicketFares(idShowtime: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${idShowtime}/fares`);
  }

  createShowtime(data: any): Observable<any> {
    return this.http.post(this.apiUrl, data);
  }

  updateShowtime(id: string, data: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, data);
  }

  cancelShowtime(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, { responseType: 'text' });
  }
}
