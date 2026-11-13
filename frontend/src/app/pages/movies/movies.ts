import { Component, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MovieService, Movie } from '../../services/movie.service';
import { environment } from '../../environments/environment';
import { TarjetaPelicula } from '../../shared/components/tarjeta-pelicula/tarjeta-pelicula';

type Pestana = 'Cartelera' | 'Estreno' | 'Preventa';

interface Funcion {
  movieId: string;
  idVenue: number;
  showDate: string;
  startTime: string;
  status: string;
}

@Component({
  selector: 'app-movies',
  standalone: true,
  imports: [RouterModule, TarjetaPelicula],
  templateUrl: './movies.html',
  styleUrl: './movies.css'
})
export class Movies implements OnInit {

  readonly pestanas: { valor: Pestana; texto: string }[] = [
    { valor: 'Cartelera', texto: 'En cartelera' },
    { valor: 'Estreno', texto: 'Estrenos' },
    { valor: 'Preventa', texto: 'Preventa' },
  ];

  private peliculas = signal<Movie[]>([]);
  private funciones = signal<Funcion[]>([]);
  cines = signal<{ idVenue: number; nameVenue: string }[]>([]);
  cargando = signal(true);
  error = signal(false);

  pestana = signal<Pestana>('Cartelera');
  busqueda = signal('');
  cine = signal<number | null>(null);

  // Con un cine elegido, solo las películas con funciones por venir en ese cine.
  private conFuncionEnElCine = computed(() => {
    const cine = this.cine();
    if (!cine) return null;
    const ahora = new Date();
    return new Set(this.funciones()
      .filter(f => f.idVenue === cine && f.status === 'Programada'
        && new Date(`${f.showDate}T${f.startTime}`) > ahora)
      .map(f => f.movieId));
  });

  resultado = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    const enCine = this.conFuncionEnElCine();
    return this.peliculas().filter(m =>
      m.isActive !== false
      && (m.status === this.pestana() || (this.pestana() === 'Cartelera' && m.status === 'Estreno'))
      && (!texto || m.titleMovie.toLowerCase().includes(texto))
      && (!enCine || enCine.has(m.idMovie)));
  });

  nombreCine = computed(() => this.cines().find(c => c.idVenue === this.cine())?.nameVenue ?? null);

  constructor(
    private movieService: MovieService,
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      this.cine.set(Number(params.get('idVenue')) || null);
    });
    this.movieService.getAllMovies().subscribe({
      next: data => { this.peliculas.set(data); this.cargando.set(false); },
      error: () => { this.error.set(true); this.cargando.set(false); }
    });
    this.http.get<Funcion[]>(`${environment.apiUrl}/showtimes`).subscribe({
      next: data => this.funciones.set(data), error: () => this.funciones.set([])
    });
    this.http.get<{ idVenue: number; nameVenue: string }[]>(`${environment.apiUrl}/venues/public`).subscribe({
      next: data => this.cines.set(data), error: () => this.cines.set([])
    });
  }

  elegirCine(valor: string): void {
    const id = Number(valor) || null;
    this.router.navigate([], { queryParams: { idVenue: id }, queryParamsHandling: 'merge', replaceUrl: true });
  }
}
