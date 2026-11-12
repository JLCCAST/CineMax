import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MovieService, Movie } from '../../services/movie.service';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home implements OnInit {

  movies: Movie[] = [];
  sedes: any[] = [];

  // "En cartelera" muestra las primeras cuatro películas reales en cartelera o en estreno.
  private peliculas = signal<Movie[]>([]);
  destacadas = computed(() => this.peliculas()
    .filter(m => m.isActive !== false && (m.status === 'Cartelera' || m.status === 'Estreno'))
    .slice(0, 4));
  readonly posterGenerico = 'Imagenes/poster-generico.svg';
  loading = true;
  error = false;

  filtroIdMovie: number | null = null;
  filtroIdVenue: number | null = null;

  constructor(
    private router: Router,
    private movieService: MovieService,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.cargarMovies();
    this.cargarSedes();
  }

  cargarMovies() {
    this.movieService.getMovies().subscribe({
      next: (data: Movie[]) => {
        this.movies = data;
        this.peliculas.set(data);
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  cargarSedes() {
    this.http.get<any[]>(`${environment.apiUrl}/venues/public`).subscribe({
      next: (data) => this.sedes = data,
      error: () => this.sedes = []
    });
  }

  filtrar() {
    const params: any = {};
    if (this.filtroIdMovie) params['idMovie'] = this.filtroIdMovie;
    if (this.filtroIdVenue) params['idVenue'] = this.filtroIdVenue;
    this.router.navigate(['/movies'], { queryParams: params });
  }

  irAMovies() {
    this.router.navigate(['/movies']);
  }
}