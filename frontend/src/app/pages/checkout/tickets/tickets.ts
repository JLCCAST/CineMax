import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { BookingResumen, BookingService, TicketEntry } from '../../../services/booking';
import { ShowtimeService } from '../../../services/showtime.service';
import { BookingSummaryComponent } from '../shared/booking-summary/booking-summary';
import { CheckoutStepperComponent } from '../shared/checkout-stepper/checkout-stepper';

interface FareRow {
  categoryCode: string;
  categoryName: string;
  descripcion: string;
  precioUnitario: number;
  cantidad: number;
}

@Component({
  selector: 'app-tickets',
  standalone: true,
  imports: [DecimalPipe, BookingSummaryComponent, CheckoutStepperComponent],
  templateUrl: './tickets.html',
  styleUrl: './tickets.css'
})
export class Tickets implements OnInit {

  resumen!: BookingResumen;
  cantidadAsientos = 0;

  filas: FareRow[] = [];
  cargando = true;
  error = '';

  private readonly DESCRIPCIONES: Record<string, string> = {
    ADULTO:       'Válido para mayores de 12 años',
    NINO:         'De 2 a 11 años (Requiere DNI)',
    ADULTO_MAYOR: 'Mayores de 60 años (Requiere DNI)',
    DISCAPACITADO:'Con certificado de discapacidad'
  };

  constructor(
    private router: Router,
    private bookingService: BookingService,
    private showtimeService: ShowtimeService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.resumen          = this.bookingService.obtenerResumen();
    this.cantidadAsientos = this.resumen.asientos?.length ?? 0;

    if (this.cantidadAsientos === 0) {
      this.router.navigate(['/seats']);
      return;
    }

    const idShowtime = this.bookingService.idShowtime;
    if (!idShowtime) {
      this.error   = 'No se encontró la función. Vuelve a la cartelera.';
      this.cargando = false;
      return;
    }

    this.showtimeService.getTicketFares(idShowtime).subscribe({
      next: (tarifas) => {
        this.filas = tarifas.map((t: any) => ({
          categoryCode:   t.categoryCode,
          categoryName:   t.categoryName,
          descripcion:    this.DESCRIPCIONES[t.categoryCode] ?? '',
          precioUnitario: Number(t.price),
          cantidad:       0
        }));

        // Restaurar tickets previos si el usuario regresó del siguiente paso
        const ticketsPrevios = this.resumen.tickets;
        if (ticketsPrevios?.length) {
          ticketsPrevios.forEach((saved: TicketEntry) => {
            const fila = this.filas.find(f => f.categoryCode === saved.categoryCode);
            if (fila) fila.cantidad = saved.cantidad;
          });
        }

        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error   = 'No se pudieron cargar las tarifas. Intenta de nuevo.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  get totalElegidos(): number {
    return this.filas.reduce((s, f) => s + f.cantidad, 0);
  }

  get totalPagar(): number {
    return this.filas.reduce((s, f) => s + f.cantidad * f.precioUnitario, 0);
  }

  get faltanEntradas(): number {
    return this.cantidadAsientos - this.totalElegidos;
  }

  get resumenEnVivo(): BookingResumen {
    return { ...this.resumen, tickets: this.ticketsElegidos() };
  }

  private ticketsElegidos(): TicketEntry[] {
    return this.filas
      .filter(f => f.cantidad > 0)
      .map(f => ({
        categoryCode:   f.categoryCode,
        categoryName:   f.categoryName,
        cantidad:       f.cantidad,
        precioUnitario: f.precioUnitario,
        subtotal:       f.cantidad * f.precioUnitario
      }));
  }

  cambiarCantidad(fila: FareRow, delta: number): void {
    const nueva = fila.cantidad + delta;
    if (nueva < 0) return;
    if (delta > 0 && this.totalElegidos >= this.cantidadAsientos) return;
    fila.cantidad = nueva;
  }

  irSiguientePaso(): void {
    if (this.totalElegidos < this.cantidadAsientos) return;

    this.bookingService.guardarTickets(this.ticketsElegidos());
    this.router.navigate(['/snacks']);
  }

  regresar(): void {
    this.router.navigate(['/seats']);
  }
}