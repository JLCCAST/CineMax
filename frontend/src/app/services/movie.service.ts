import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

export interface Genre {
  idGenre: string;
  nameGenre: string;
}

export interface Classification {
  idClassification: string;
  nameClassification: string;
  descriptionText: string;
}

export interface Movie {
  idMovie: string;
  titleMovie: string;
  synopsis: string;
  durationMinutes: number;
  posterUrl: string;
  releaseDate: string;
  director: string;
  status: string;
  isActive?: boolean;
  premiereWeek: boolean;
  genres?: Genre[];
  classification?: Classification;
}

@Injectable({
  providedIn: 'root'
})
export class MovieService {
  private apiUrl = `${environment.apiUrl}/movies`;

  constructor(private http: HttpClient) {}

  getMovies(): Observable<Movie[]> {
    return this.http.get<Movie[]>(`${this.apiUrl}/status/Cartelera`);
  }

  // Todas, en cualquier estado (cartelera, estreno, preventa).
  getAllMovies(): Observable<Movie[]> {
    return this.http.get<Movie[]>(this.apiUrl);
  }

  getMovieById(id: string): Observable<Movie> {
    return this.http.get<Movie>(`${this.apiUrl}/${id}`);
  }

  getMoviesByStatus(status: string): Observable<Movie[]> {
    return this.http.get<Movie[]>(`${this.apiUrl}/status/${status}`);
  }

  createMovie(formData: FormData): Observable<any> {
    return this.http.post(`${this.apiUrl}`, formData);
  }

  updateMovie(id: string, formData: FormData): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, formData);
  }

  deleteMovie(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}