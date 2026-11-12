import { Component, OnInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { BookingService } from '../../services/booking';
import { MovieService } from '../../services/movie.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-movie-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './movie-detail.html',
  styleUrl: './movie-detail.css'
})
export class MovieDetail implements OnInit {
  movie: any = null;
  loading = true;
  error = false;

  horarios: any[] = [];
  sedesAgrupadas: any[] = [];
  showtimeSeleccionado: string | null = null;
  formatoSeleccionado: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookingService: BookingService,
    private movieService: MovieService,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef,
    private http: HttpClient
  ) {}

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.movie = null;
      this.loading = true;
      this.error = false;
      const id = params.get('id')!;
      this.movieService.getMovieById(id).subscribe({
        next: (data) => {
          this.movie = data;
          this.loading = false;
          this.cdr.detectChanges();
          this.cargarHorarios(id);
        },
        error: () => {
          this.error = true;
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    });
  }

  cargarHorarios(idMovie: string) {
    this.http.get<any[]>(`${environment.apiUrl}/showtimes?idMovie=${idMovie}`).subscribe({
      next: (data) => {
        this.horarios = data;
        this.agruparPorSede();
        this.cdr.detectChanges();
      },
      error: () => {
        this.horarios = [];
        this.sedesAgrupadas = [];
        this.cdr.detectChanges();
      }
    });
  }

  agruparPorSede() {
    const mapa = new Map<string, any>();

    for (const h of this.horarios) {
      if (!mapa.has(h.nameVenue)) {
        mapa.set(h.nameVenue, { nameVenue: h.nameVenue, formatos: new Map() });
      }
      const sede = mapa.get(h.nameVenue);

      if (!sede.formatos.has(h.languageFormat)) {
        sede.formatos.set(h.languageFormat, []);
      }
      sede.formatos.get(h.languageFormat).push({
        idShowtime: h.idShowtime,
        hora: h.startTime,
        precio: h.baseTicketPrice
      });
    }

    this.sedesAgrupadas = Array.from(mapa.values()).map(s => ({
      nameVenue: s.nameVenue,
      formatos: (Array.from(s.formatos.entries()) as [string, any][]).map(([formato, horas]) => ({ formato, horas }))
    }));
  }

  seleccionarHorario(idShowtime: string, formato: string) {
    this.showtimeSeleccionado = idShowtime;
    this.formatoSeleccionado = formato;
  }

  volver() {
    this.router.navigate(['/movies']);
  }

  empezarCompra() {
    if (!this.showtimeSeleccionado) {
      alert('Por favor selecciona un horario primero.');
      return;
    }
    this.bookingService.iniciarReserva(this.showtimeSeleccionado);
    this.router.navigate(['/seats'], { queryParams: { show: this.showtimeSeleccionado } });
  }
}