import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { MovieService, Movie } from '../../services/movie.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-movies',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './movies.html',
  styleUrl: './movies.css'
})
export class Movies implements OnInit {

  tabActivo: string = 'Cartelera';
  tabs = ['Cartelera', 'Estreno', 'Preventa'];

  todasLasPeliculas: Movie[] = [];
  peliculasFiltradas: Movie[] = [];
  loading = false;
  error = false;

  textoBusqueda: string = '';
  clasificacionSeleccionada: string = '';
  clasificaciones: string[] = ['ATP', '+14', '+17', 'PG-13'];
  mensajeFiltro: string = '';

  filtroIdMovie: string | null = null;
  filtroIdVenue: number | null = null;

  constructor(
    private movieService: MovieService,
    private cdr: ChangeDetectorRef,
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.filtroIdMovie = params['idMovie'] ? params['idMovie'] : null;
      this.filtroIdVenue = params['idVenue'] ? +params['idVenue'] : null;
      this.cargarPorTab('Cartelera');
    });
  }

  cargarPorTab(tab: string) {
    this.tabActivo = tab;
    this.loading = true;
    this.error = false;
    this.todasLasPeliculas = [];
    this.peliculasFiltradas = [];
    this.textoBusqueda = '';
    this.clasificacionSeleccionada = '';
    this.mensajeFiltro = '';
    this.cdr.detectChanges();

    if (tab === 'Cartelera') {
      this.movieService.getMoviesByStatus('Cartelera').subscribe({
        next: (cartelera) => {
          this.movieService.getMoviesByStatus('Estreno').subscribe({
            next: (estrenos) => {
              this.todasLasPeliculas = [...cartelera, ...estrenos];
              this.aplicarFiltrosIniciales();
              this.loading = false;
              this.cdr.detectChanges();
            },
            error: () => {
              this.todasLasPeliculas = [...cartelera];
              this.aplicarFiltrosIniciales();
              this.loading = false;
              this.cdr.detectChanges();
            }
          });
        },
        error: () => {
          this.error = true;
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    } else {
      this.movieService.getMoviesByStatus(tab as any).subscribe({
        next: (data: Movie[]) => {
          this.todasLasPeliculas = [...data];
          this.aplicarFiltrosIniciales();
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.error = true;
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  aplicarFiltrosIniciales() {
    if (this.filtroIdMovie) {
      this.peliculasFiltradas = this.todasLasPeliculas.filter(p => p.idMovie === this.filtroIdMovie);
      this.mensajeFiltro = `${this.peliculasFiltradas.length} película(s) encontrada(s).`;
    } else if (this.filtroIdVenue) {
      // Filtra por sede usando showtimes
      // Fecha local (en-CA da AAAA-MM-DD): toISOString usa UTC y después de las 19:00 en Lima ya es mañana.
      const hoy = new Date().toLocaleDateString('en-CA');
      this.http.get<any[]>(`${environment.apiUrl}/showtimes/by-venue?idVenue=${this.filtroIdVenue}&date=${hoy}`).subscribe({
        next: (showtimes) => {
          const idsConFuncion = new Set(showtimes.map((s: any) => s.movieId));
          this.peliculasFiltradas = this.todasLasPeliculas.filter(p => idsConFuncion.has(p.idMovie));
          this.mensajeFiltro = `${this.peliculasFiltradas.length} película(s) en esta sede hoy.`;
          this.cdr.detectChanges();
        },
        error: () => {
          this.peliculasFiltradas = [...this.todasLasPeliculas];
          this.cdr.detectChanges();
        }
      });
    } else {
      this.peliculasFiltradas = [...this.todasLasPeliculas];
    }
  }

  onBusqueda(valor: string) {
    this.textoBusqueda = valor;
    this.aplicarFiltros();
  }

  onClasificacion(valor: string) {
    this.clasificacionSeleccionada = valor;
    this.aplicarFiltros();
  }

  aplicarFiltros() {
    const texto = this.textoBusqueda.trim().toLowerCase();
    const clasif = this.clasificacionSeleccionada;

    this.peliculasFiltradas = this.todasLasPeliculas.filter(p => {
      const coincideTexto = texto === '' || p.titleMovie.toLowerCase().includes(texto);
      const coincideClasif = clasif === '' || (p as any).classification?.nameClassification === clasif;
      return coincideTexto && coincideClasif;
    });

    if (this.peliculasFiltradas.length === 0) {
      this.mensajeFiltro = 'No se encontraron películas con esos filtros.';
    } else {
      this.mensajeFiltro = `${this.peliculasFiltradas.length} película(s) encontrada(s).`;
    }
  }

  limpiarFiltros() {
    this.textoBusqueda = '';
    this.clasificacionSeleccionada = '';
    this.mensajeFiltro = '';
    this.filtroIdMovie = null;
    this.filtroIdVenue = null;
    this.peliculasFiltradas = [...this.todasLasPeliculas];
  }

  irADetalle(id: string) {
    window.location.href = '/movie/' + id;
  }
}