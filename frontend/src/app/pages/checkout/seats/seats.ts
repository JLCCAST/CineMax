import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { BookingService } from '../../../services/booking';
import { AuthService } from '../../../services/auth.service';
import { ShowtimeService } from '../../../services/showtime.service';
import { SeatService } from '../../../services/seat.service';

import { Offcanvas } from 'bootstrap';

interface SeatCell {
  idSeat: number;
  codigo: string;
  fila: string;
  columna: number;
  estado: 'disponible' | 'ocupado' | 'mantenimiento' | 'seleccionado' | 'wheelchair';
  esOculto: boolean;
}

@Component({
  selector: 'app-seats',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './seats.html',
  styleUrl: './seats.css'
})
export class Seats implements OnInit {

  idShowtime: string | null = null;
  resumen: any = null;

  filas: string[] = [];
  columnas: number[] = [];
  matrizAsientos: Map<string, SeatCell> = new Map();

  seleccionados: SeatCell[] = [];
  readonly MAX_ASIENTOS = 10;

  cargando = true;
  error = '';

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private bookingService: BookingService,
    private authService: AuthService,
    private seatService: SeatService,
    private showtimeService: ShowtimeService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const paramId = this.route.snapshot.queryParamMap.get('show')
      ?? this.route.snapshot.queryParamMap.get('idShowtime');
    if (paramId) {
      this.idShowtime = paramId;
      this.bookingService.iniciarReserva(this.idShowtime);
    } else if (this.bookingService.idShowtime) {
      this.idShowtime = this.bookingService.idShowtime;
    } else {
      this.error = 'No se especificó una función. Vuelve a la cartelera.';
      this.cargando = false;
      return;
    }

    this.resumen = this.bookingService.obtenerResumen();
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando = true;

    this.showtimeService.getShowtimeSummary(this.idShowtime!).subscribe({
      next: (data) => {
        const fechaHora = `${data.showDate} ${data.startTime}`;
        this.bookingService.guardarInfoShowtime({
          nombre: data.titleMovie,
          formato: data.languageFormat,
          poster: data.posterUrl,
          cine: data.nameVenue,
          sala: data.nameRoom,
          idVenue: data.idVenue,
          fechaHora
        });
        this.resumen = this.bookingService.obtenerResumen();
        this.cdr.detectChanges();
      },
      error: () => this.error = 'No se pudo cargar la información de la función.'
    });

    this.seatService.getSeatsStatusByShowtime(this.idShowtime!).subscribe({
      next: (asientos) => {
        this.construirMatriz(asientos);

        // ── RESTAURAR asientos previos si el usuario regresó de tickets ──
        const resumen = this.bookingService.obtenerResumen();
        if (resumen.asientosIds?.length) {
          resumen.asientosIds.forEach((id: number) => {
            this.matrizAsientos.forEach(cell => {
              if (cell.idSeat === id && !cell.esOculto &&
                  cell.estado !== 'ocupado' && cell.estado !== 'mantenimiento') {
                if (!this.seleccionados.some(s => s.idSeat === id)) {
                  this.seleccionados.push(cell);
                }
              }
            });
          });
        }

        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudieron cargar los asientos.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  construirMatriz(asientos: any[]): void {
    this.matrizAsientos.clear();

    const filasSet = new Set<string>();
    const colsSet  = new Set<number>();

    asientos.forEach(a => {
      filasSet.add(a.rowLetter);
      colsSet.add(a.columnNumber);
    });

    this.filas    = Array.from(filasSet).sort();
    this.columnas = Array.from(colsSet).sort((a, b) => a - b);

    asientos.forEach(a => {
      const codigo = `${a.rowLetter}${a.columnNumber}`;
      let estado: SeatCell['estado'] = 'disponible';

      if (a.status === 'MANTENIMIENTO') {
        estado = 'mantenimiento';
      } else if (a.isOccupied) {
        estado = 'ocupado';
      } else if (a.nameSeatType === 'WHEELCHAIR') {
        estado = 'wheelchair';
      }

      this.matrizAsientos.set(codigo, {
        idSeat: a.idSeat,
        codigo,
        fila: a.rowLetter,
        columna: a.columnNumber,
        estado,
        esOculto: a.status === 'OCULTO'
      });
    });
  }

  getAsiento(fila: string, col: number): SeatCell | undefined {
    return this.matrizAsientos.get(`${fila}${col}`);
  }

  getEstadoVisible(cell: SeatCell): string {
    if (this.seleccionados.some(s => s.idSeat === cell.idSeat)) return 'seleccionado';
    return cell.estado;
  }

  toggleAsiento(cell: SeatCell): void {
    if (cell.esOculto) return;
    if (cell.estado === 'ocupado' || cell.estado === 'mantenimiento') return;

    const idx = this.seleccionados.findIndex(s => s.idSeat === cell.idSeat);
    if (idx > -1) {
      this.seleccionados.splice(idx, 1);
    } else {
      if (this.seleccionados.length >= this.MAX_ASIENTOS) {
        alert(`Solo puedes seleccionar un máximo de ${this.MAX_ASIENTOS} asientos por compra.`);
        return;
      }
      this.seleccionados.push(cell);
    }
  }

  estaSeleccionado(cell: SeatCell): boolean {
    return this.seleccionados.some(s => s.idSeat === cell.idSeat);
  }

  get codigosSeleccionados(): string {
    if (!this.seleccionados.length) return 'Ninguno';
    return this.seleccionados.map(s => s.codigo).join(', ');
  }

  irSiguientePaso(): void {
    if (!this.seleccionados.length) {
      alert('Por favor selecciona al menos un asiento.');
      return;
    }

    if (!this.authService.isLoggedIn()) {
      const offcanvasEl = document.getElementById('authOffcanvas');
      if (offcanvasEl) {
        const offcanvas = Offcanvas.getOrCreateInstance(offcanvasEl);
        offcanvas.show();
      }
      return;
    }

    const codigos = this.seleccionados.map(s => s.codigo);
    const ids     = this.seleccionados.map(s => s.idSeat);

    this.bookingService.guardarAsientosYLimpiarSiguientes(codigos, ids);
    this.router.navigate(['/tickets']);
  }

  cancelar(): void {
    this.bookingService.limpiar();
    this.router.navigate(['/movies']);
  }
}