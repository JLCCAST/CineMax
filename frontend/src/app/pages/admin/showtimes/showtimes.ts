import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ShowtimeService } from '../../../services/showtime.service';
import { MovieService } from '../../../services/movie.service';
import { RoomService } from '../../../services/room.service';
import { VenueService } from '../../../services/venue.service';
import { AuthService } from '../../../services/auth.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-showtimes',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './showtimes.html',
  styleUrls: ['./showtimes.css']
})
export class AdminShowtimes implements OnInit {

  // Sesión
  idVenueSesion: number | null = null;
  roleUsuario = '';
  esGerGeneral = false;

  // Catálogos
  movies: any[] = [];
  rooms: any[] = [];
  sedes: any[] = [];
  sedeSeleccionadaId: number | null = null;

  // Lista y filtros (estos siguen con ngModel porque son filtros, no un form de negocio)
  showtimes: any[] = [];
  filtroFecha = '';
  filtroSala: number | null = null;
  filtroFormato = '';

  cargando = false;
  guardando = false;
  mensajeError = '';
  mensajeExito = '';
  formEnviado = false;

  isEditMode = false;
  currentShowtimeId: string | null = null;
  horaFinEstimada = '';

  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private showtimeService: ShowtimeService,
    private movieService: MovieService,
    private roomService: RoomService,
    private venueService: VenueService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.filtroFecha = new Date().toISOString().split('T')[0];
    this.idVenueSesion = this.authService.getIdVenue();
    this.roleUsuario   = this.authService.getRole();
    this.esGerGeneral  = this.idVenueSesion === null;

    this.construirForm();
    this.cargarPeliculas();

    if (this.esGerGeneral) {
      this.cargarSedes();
    } else {
      this.cargarSalas(this.idVenueSesion!);
    }
  }

  // ── Formulario ────────────────────────────────────────────────────────────

  private fechaNoPassadaValidator() {
    return (control: import('@angular/forms').AbstractControl) => {
      if (!control.value) return null; // si está vacío, 'required' ya lo captura — no apilar errores
      const hoy = new Date().toISOString().split('T')[0];
      return control.value < hoy ? { fechaPasada: true } : null;
    };
  }
  
  private horaNoPassadaValidator() {
    return (control: import('@angular/forms').AbstractControl) => {
      const form = control.parent;
      if (!form) return null;
      const fecha = form.get('showDate')?.value;
      const hora  = control.value;
      if (!fecha || !hora) return null;
  
      const hoy  = new Date().toISOString().split('T')[0];
      if (fecha !== hoy) return null; // solo aplica si la fecha ES hoy
  
      const [h, m] = hora.split(':').map(Number);
      const ahora  = new Date();
      const minutosIngresados = h * 60 + m;
      const minutosActuales   = ahora.getHours() * 60 + ahora.getMinutes();
      return minutosIngresados <= minutosActuales ? { horaPassada: true } : null;
    };
  }
  
  private construirForm(): void {
    this.form = this.fb.group({
      idMovie:         [null, Validators.required],
      idRoom:          [null, Validators.required],
      showDate:        [this.filtroFecha, [Validators.required, this.fechaNoPassadaValidator()]],
      startTime:       ['',  [Validators.required, this.horaNoPassadaValidator()]],
      languageFormat:  ['Doblada 2D', Validators.required],
      baseTicketPrice: [15.00, [Validators.required, Validators.min(1), Validators.max(999)]]
    });
  
    this.form.get('idMovie')!.valueChanges.subscribe(() => this.calcularHoraFin());
  
    // Cuando cambia la fecha, re-validar la hora (puede haber quedado inválida)
    this.form.get('showDate')!.valueChanges.subscribe(() => {
      this.form.get('startTime')!.updateValueAndValidity();
      this.calcularHoraFin();
    });
  
    this.form.get('startTime')!.valueChanges.subscribe(() => this.calcularHoraFin());
  }

  ctrl(name: string) { return this.form.get(name)!; }

  invalido(name: string): boolean {
    const c = this.ctrl(name);
    return c.invalid && (c.touched || this.formEnviado);
  }

  // ── Hora fin estimada ─────────────────────────────────────────────────────

  calcularHoraFin(): void {
    const movie = this.movies.find(m => m.idMovie === this.ctrl('idMovie').value);
    const startTime = this.ctrl('startTime').value;
    if (!movie || !startTime) { this.horaFinEstimada = ''; return; }

    const [h, m] = startTime.split(':').map(Number);
    const totalMin = h * 60 + m + movie.durationMinutes + 30;
    const hFin = Math.floor(totalMin / 60) % 24;
    const mFin = totalMin % 60;
    this.horaFinEstimada = `${String(hFin).padStart(2, '0')}:${String(mFin).padStart(2, '0')}`;
  }

  // ── Tarifas estimadas (computed) ──────────────────────────────────────────

  get precioBase(): number { return +(this.ctrl('baseTicketPrice').value ?? 0); }
  // Misma regla que cartelera (Tarifas.java): porcentaje del precio base, redondeado a S/ 0,50.
  get precioNino(): number  { return Math.round(this.precioBase * 0.55 * 2) / 2; }
  get precioMayor(): number { return Math.round(this.precioBase * 0.65 * 2) / 2; }

  // ── Datos ─────────────────────────────────────────────────────────────────

  cargarPeliculas(): void {
    this.movieService.getMoviesByStatus('Cartelera').subscribe(res => {
      this.movies = res;
      this.cdr.detectChanges();
    });
  }

  cargarSedes(): void {
    this.venueService.getVenues().subscribe(res => {
      this.sedes = res;
      this.cdr.detectChanges();
    });
  }

  cargarSalas(idVenue: number): void {
    this.roomService.getRoomsByVenue(idVenue).subscribe(res => {
      this.rooms = (res as any[]).filter(r => r.status === 'Activo');
      this.cdr.detectChanges();
      this.cargarFunciones();
    });
  }

  onSedeChange(): void {
    this.rooms = [];
    this.showtimes = [];
    this.filtroSala = null;
    if (this.sedeSeleccionadaId) this.cargarSalas(this.sedeSeleccionadaId);
  }

  get idVenueEfectivo(): number | null {
    return this.esGerGeneral ? this.sedeSeleccionadaId : this.idVenueSesion;
  }

  cargarFunciones(): void {
    if (!this.idVenueEfectivo || !this.filtroFecha) return;
    this.cargando = true;
    this.showtimeService.getShowtimesByVenue(this.idVenueEfectivo, this.filtroFecha).subscribe({
      next: res => { this.showtimes = res; this.cargando = false; this.cdr.detectChanges(); },
      error: err => { this.cargando = false; this.mostrarError(this.extraerMensaje(err, 'Error al cargar funciones.')); }
    });
  }

  get funcionesFiltradas(): any[] {
    return this.showtimes.filter(f => {
      const porSala    = !this.filtroSala    || f.idRoom === +this.filtroSala;
      const porFormato = !this.filtroFormato || f.languageFormat === this.filtroFormato;
      return porSala && porFormato;
    });
  }

  get formatos(): string[] {
    return Array.from(new Set(this.showtimes.map(f => f.languageFormat)));
  }

  // ── Modales ───────────────────────────────────────────────────────────────

  abrirModalNuevo(): void {
    this.isEditMode = false;
    this.currentShowtimeId = null;
    this.mensajeError = '';
    this.mensajeExito = '';
    this.horaFinEstimada = '';
    this.formEnviado = false;
    this.form.reset({
      idMovie: null, idRoom: null,
      showDate: this.filtroFecha,
      startTime: '', languageFormat: 'Doblada 2D', baseTicketPrice: 15.00
    });
  }

  abrirModalEditar(f: any): void {
    this.isEditMode = true;
    this.currentShowtimeId = f.idShowtime;
    this.mensajeError = '';
    this.mensajeExito = '';
    this.formEnviado = false;
    this.form.setValue({
      idMovie:         f.movieId,
      idRoom:          f.idRoom,
      showDate:        f.showDate,
      startTime:       f.startTime,
      languageFormat:  f.languageFormat,
      baseTicketPrice: f.baseTicketPrice
    });
    this.calcularHoraFin();
  }

  // ── Guardar ───────────────────────────────────────────────────────────────

  guardarFuncion(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
    this.formEnviado = true;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.guardando = true;
    this.calcularHoraFin();
    const payload = {
      movieId:         this.ctrl('idMovie').value,
      idRoom:          +this.ctrl('idRoom').value,
      showDate:        this.ctrl('showDate').value,
      startTime:       this.ctrl('startTime').value,
      endTime:         this.horaFinEstimada,
      languageFormat:  this.ctrl('languageFormat').value,
      baseTicketPrice: +this.ctrl('baseTicketPrice').value
    };

    const op = this.isEditMode
      ? this.showtimeService.updateShowtime(this.currentShowtimeId!, payload)
      : this.showtimeService.createShowtime(payload);

    op.subscribe({
      next: () => {
        this.guardando = false;
        this.mensajeExito = this.isEditMode ? 'Función actualizada.' : 'Función programada correctamente.';
        this.cargarFunciones();
        setTimeout(() => this.cerrarModal(), 1500);
      },
      error: err => { this.guardando = false; this.mostrarError(this.extraerMensaje(err, 'Error al guardar.')); }
    });
  }

  cancelarFuncion(f: any): void {
    if (!confirm(`¿Cancelar la función de "${f.movieTitle}" el ${f.showDate} a las ${f.startTime}?`)) return;
    this.showtimeService.cancelShowtime(f.idShowtime).subscribe({
      next: () => { this.mensajeExito = 'Función cancelada.'; this.cargarFunciones(); },
      error: err => this.mostrarError(this.extraerMensaje(err, 'Error al cancelar.'))
    });
  }

  cerrarModal(): void { document.getElementById('btnCerrarModal')?.click(); }

  mostrarError(msg: string): void { this.mensajeError = msg; this.cdr.detectChanges(); }

  extraerMensaje(err: any, fallback: string): string {
    const body = err?.error;
    if (typeof body === 'string' && body.trim()) return body;
    if (typeof body === 'object' && body) return body.message || body.error || JSON.stringify(body);
    return err?.message || fallback;
  }

  getBadgeClase(status: string): string {
    const map: Record<string, string> = {
      'Programada': 'badge bg-success',
      'En Curso':   'badge bg-warning text-dark',
      'Finalizada': 'badge bg-secondary',
      'Cancelada':  'badge bg-danger'
    };
    return map[status] ?? 'badge bg-light text-dark';
  }
}