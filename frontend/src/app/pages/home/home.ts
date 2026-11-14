import { Component, Injector, OnDestroy, OnInit, afterNextRender, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MovieService, Movie } from '../../services/movie.service';
import { BookingService } from '../../services/booking';
import { environment } from '../../environments/environment';
import { TarjetaPelicula } from '../../shared/components/tarjeta-pelicula/tarjeta-pelicula';

interface Funcion {
  idShowtime: string;
  movieId: string;
  idVenue: number;
  nameVenue: string;
  showDate: string;
  startTime: string;
  languageFormat: string;
  status: string;
}

interface Sede {
  idVenue: number;
  nameVenue: string;
  addressVenue: string;
  districtName: string;
  imageUrl?: string | null;
}

interface Promocion {
  idPromotion: number;
  namePromotion: string;
  description: string | null;
  promotionCode: string | null;
  imageUrl: string | null;
  appliesTo: string;
}

type Pestana = 'Cartelera' | 'Estreno' | 'Preventa';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, TarjetaPelicula],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home implements OnInit, OnDestroy {

  readonly posterGenerico = 'Imagenes/poster-generico.svg';
  readonly pestanas: { valor: Pestana; texto: string }[] = [
    { valor: 'Cartelera', texto: 'En cartelera' },
    { valor: 'Estreno', texto: 'Estrenos' },
    { valor: 'Preventa', texto: 'Preventa' },
  ];

  private peliculas = signal<Movie[]>([]);
  private funciones = signal<Funcion[]>([]);
  sedes = signal<Sede[]>([]);
  promociones = signal<Promocion[]>([]);
  cargando = signal(true);

  // ── Portada destacada ──────────────────────────────────────────────────────
  destacadas = computed(() => this.peliculas()
    .filter(m => m.isActive !== false && (m.status === 'Cartelera' || m.status === 'Estreno'))
    .slice(0, 5));
  indiceDestacada = signal(0);
  destacada = computed(() => this.destacadas()[this.indiceDestacada()] ?? null);
  private rotacion?: ReturnType<typeof setInterval>;

  // ── Cartelera ──────────────────────────────────────────────────────────────
  pestana = signal<Pestana>('Cartelera');
  // "En cartelera" incluye los estrenos: también tienen funciones.
  cartelera = computed(() => this.peliculas()
    .filter(m => m.isActive !== false && (m.status === this.pestana()
      || (this.pestana() === 'Cartelera' && m.status === 'Estreno'))));

  // ── Compra rápida: película → sede → día → horario ──────────────────────────
  peliculaElegida = signal<string | null>(null);
  sedeElegida = signal<number | null>(null);
  diaElegido = signal<string | null>(null);
  funcionElegida = signal<string | null>(null);

  private funcionesFuturas = computed(() => this.funciones()
    .filter(f => f.status === 'Programada' && !Home.yaEmpezo(f)));
  peliculasConFunciones = computed(() => {
    const ids = new Set(this.funcionesFuturas().map(f => f.movieId));
    return this.peliculas().filter(m => ids.has(m.idMovie));
  });
  sedesDeLaPelicula = computed(() => {
    const vistas = new Map<number, string>();
    this.funcionesFuturas()
      .filter(f => f.movieId === this.peliculaElegida())
      .forEach(f => vistas.set(f.idVenue, f.nameVenue));
    return [...vistas.entries()].map(([idVenue, nameVenue]) => ({ idVenue, nameVenue }));
  });
  dias = computed(() => [...new Set(this.funcionesFuturas()
    .filter(f => f.movieId === this.peliculaElegida() && f.idVenue === this.sedeElegida())
    .map(f => f.showDate))].sort());
  horarios = computed(() => this.funcionesFuturas()
    .filter(f => f.movieId === this.peliculaElegida() && f.idVenue === this.sedeElegida()
      && f.showDate === this.diaElegido())
    .sort((a, b) => a.startTime.localeCompare(b.startTime)));

  private readonly injector = inject(Injector);
  private fragmento: string | null = null;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private movieService: MovieService,
    private bookingService: BookingService,
    private http: HttpClient
  ) {
    this.route.fragment.pipe(takeUntilDestroyed()).subscribe(f => {
      this.fragmento = f;
      this.irAPromociones();
    });
  }

  ngOnInit(): void {
    this.movieService.getAllMovies().subscribe({
      next: data => { this.peliculas.set(data); this.cargando.set(false); this.irAPromociones(); },
      error: () => { this.cargando.set(false); this.irAPromociones(); }
    });
    this.http.get<Funcion[]>(`${environment.apiUrl}/showtimes`).subscribe({
      next: data => this.funciones.set(data), error: () => this.funciones.set([])
    });
    this.http.get<Sede[]>(`${environment.apiUrl}/venues/public`).subscribe({
      next: data => this.sedes.set(data), error: () => this.sedes.set([])
    });
    this.http.get<Promocion[]>(`${environment.apiUrl}/promotions/vigentes`).subscribe({
      next: data => { this.promociones.set(data); this.irAPromociones(); },
      error: () => this.promociones.set([])
    });
    this.rotacion = setInterval(() => this.siguienteDestacada(), 7000);
  }

  ngOnDestroy(): void {
    clearInterval(this.rotacion);
  }

  // La sección de promociones aparece cuando llegan los datos, después de que el router
  // buscó el ancla. Se baja a ella cuando ya cargó lo que va encima (la cartelera).
  private irAPromociones(): void {
    if (this.fragmento !== 'promociones' || this.cargando() || !this.promociones().length) return;
    afterNextRender(() => document.getElementById('promociones')?.scrollIntoView({ behavior: 'smooth' }),
      { injector: this.injector });
  }

  anteriorDestacada(): void {
    const total = this.destacadas().length;
    if (total > 1) this.irADestacada((this.indiceDestacada() - 1 + total) % total);
  }

  siguienteDestacada(): void {
    const total = this.destacadas().length;
    if (total > 1) this.indiceDestacada.set((this.indiceDestacada() + 1) % total);
  }

  irADestacada(indice: number): void {
    this.indiceDestacada.set(indice);
    clearInterval(this.rotacion);
    this.rotacion = setInterval(() => this.siguienteDestacada(), 7000);
  }

  elegirPelicula(id: string): void {
    this.peliculaElegida.set(id || null);
    const sedes = this.sedesDeLaPelicula();
    this.sedeElegida.set(sedes.length === 1 ? sedes[0].idVenue : null);
    this.elegirSede(this.sedeElegida());
  }

  elegirSede(id: number | null): void {
    this.sedeElegida.set(id);
    this.diaElegido.set(this.dias()[0] ?? null);
    this.funcionElegida.set(null);
  }

  elegirDia(dia: string): void {
    this.diaElegido.set(dia);
    this.funcionElegida.set(null);
  }

  comprar(): void {
    const id = this.funcionElegida();
    if (!id) return;
    this.bookingService.iniciarReserva(id);
    this.router.navigate(['/seats'], { queryParams: { show: id } });
  }

  // ── Ayudas para la plantilla ─────────────────────────────────────────────
  generos(m: Movie): string {
    return (m.genres ?? []).map(g => g.nameGenre).slice(0, 2).join(' · ');
  }

  duracion(m: Movie): string {
    const h = Math.floor(m.durationMinutes / 60);
    const min = m.durationMinutes % 60;
    return h ? `${h} h ${min} min` : `${min} min`;
  }

  hora(f: Funcion): string {
    return f.startTime.slice(0, 5);
  }

  etiquetaDia(fecha: string): string {
    const dia = new Date(`${fecha}T00:00:00`);
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const diferencia = Math.round((dia.getTime() - hoy.getTime()) / 86_400_000);
    if (diferencia === 0) return 'Hoy';
    if (diferencia === 1) return 'Mañana';
    const texto = dia.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'short' });
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }

  ocultarImagen(evento: Event): void {
    (evento.target as HTMLImageElement).style.display = 'none';
  }

  private static yaEmpezo(f: Funcion): boolean {
    return new Date(`${f.showDate}T${f.startTime}`) <= new Date();
  }
}
