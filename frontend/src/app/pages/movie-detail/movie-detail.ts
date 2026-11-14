import { Component, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { BookingService } from '../../services/booking';
import { Movie, MovieService } from '../../services/movie.service';
import { environment } from '../../environments/environment';

interface Funcion {
  idShowtime: string;
  idVenue: number;
  nameVenue: string;
  nameRoom: string;
  showDate: string;
  startTime: string;
  languageFormat: string;
  status: string;
}

@Component({
  selector: 'app-movie-detail',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './movie-detail.html',
  styleUrl: './movie-detail.css'
})
export class MovieDetail implements OnInit {

  readonly posterGenerico = 'Imagenes/poster-generico.svg';

  movie = signal<Movie | null>(null);
  cargando = signal(true);
  error = signal(false);

  private funciones = signal<Funcion[]>([]);
  diaElegido = signal<string | null>(null);
  elegida = signal<Funcion | null>(null);

  dias = computed(() => [...new Set(this.funciones().map(f => f.showDate))].sort());

  // Funciones del día elegido, agrupadas por cine y por formato (doblada, subtitulada…).
  cines = computed(() => {
    const grupos = new Map<string, Map<string, Funcion[]>>();
    for (const f of this.funciones().filter(x => x.showDate === this.diaElegido())) {
      const formatos = grupos.get(f.nameVenue) ?? new Map<string, Funcion[]>();
      formatos.set(f.languageFormat, [...(formatos.get(f.languageFormat) ?? []), f]);
      grupos.set(f.nameVenue, formatos);
    }
    return [...grupos.entries()].map(([nombre, formatos]) => ({
      nombre,
      formatos: [...formatos.entries()].map(([formato, funciones]) => ({ formato, funciones }))
    }));
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookingService: BookingService,
    private movieService: MovieService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id')!;
      this.movie.set(null);
      this.cargando.set(true);
      this.error.set(false);
      this.elegida.set(null);
      this.movieService.getMovieById(id).subscribe({
        next: data => { this.movie.set(data); this.cargando.set(false); this.cargarFunciones(id); },
        error: () => { this.error.set(true); this.cargando.set(false); }
      });
    });
  }

  private cargarFunciones(idMovie: string): void {
    this.http.get<Funcion[]>(`${environment.apiUrl}/showtimes?idMovie=${idMovie}`).subscribe({
      next: data => {
        // Solo funciones que no han empezado, en orden de fecha y hora.
        const futuras = data
          .filter(f => f.status === 'Programada' && !MovieDetail.yaEmpezo(f))
          .sort((a, b) => (a.showDate + a.startTime).localeCompare(b.showDate + b.startTime));
        this.funciones.set(futuras);
        this.diaElegido.set(futuras[0]?.showDate ?? null);
      },
      error: () => this.funciones.set([])
    });
  }

  elegirDia(dia: string): void {
    this.diaElegido.set(dia);
    this.elegida.set(null);
  }

  comprar(): void {
    const f = this.elegida();
    if (!f) return;
    this.bookingService.iniciarReserva(f.idShowtime);
    this.router.navigate(['/seats'], { queryParams: { show: f.idShowtime } });
  }

  hora(f: Funcion): string {
    return f.startTime.slice(0, 5);
  }

  generos(m: Movie): string {
    return (m.genres ?? []).map(g => g.nameGenre).join(', ');
  }

  duracion(m: Movie): string {
    const h = Math.floor(m.durationMinutes / 60);
    const min = m.durationMinutes % 60;
    return h ? `${h} h ${min} min` : `${min} min`;
  }

  // "Hoy", "Mañana" o el día ("sáb. 15"), en la hora de Lima del navegador.
  etiquetaDia(fecha: string): { nombre: string; numero: string } {
    const dia = new Date(`${fecha}T00:00:00`);
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const diferencia = Math.round((dia.getTime() - hoy.getTime()) / 86_400_000);
    const nombre = diferencia === 0 ? 'Hoy' : diferencia === 1 ? 'Mañana'
      : dia.toLocaleDateString('es-PE', { weekday: 'short' }).replace('.', '');
    return { nombre, numero: dia.toLocaleDateString('es-PE', { day: 'numeric', month: 'short' }).replace('.', '') };
  }

  private static yaEmpezo(f: Funcion): boolean {
    return new Date(`${f.showDate}T${f.startTime}`) <= new Date();
  }
}
