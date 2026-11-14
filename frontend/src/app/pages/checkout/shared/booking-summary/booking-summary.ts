import { Component, Input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { BookingResumen } from '../../../../services/booking';

export interface BookingSummarySections {
  pelicula: boolean;
  cine: boolean;
  asientos: boolean;
  tickets: boolean;
  snacks: boolean;
  total: boolean;
}

const TODAS: BookingSummarySections = {
  pelicula: true, cine: true, asientos: true, tickets: true, snacks: true, total: true,
};

// Resumen de la compra en la columna lateral del checkout. Los botones de cada paso
// van dentro (ng-content).
@Component({
  selector: 'app-booking-summary',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './booking-summary.html',
  styleUrl: './booking-summary.css'
})
export class BookingSummaryComponent {

  readonly posterGenerico = 'Imagenes/poster-generico.svg';

  @Input() resumen!: BookingResumen;

  @Input() set secciones(valor: Partial<BookingSummarySections>) {
    this._secciones = { ...TODAS, ...valor };
  }
  get secciones(): BookingSummarySections { return this._secciones; }
  private _secciones: BookingSummarySections = { ...TODAS };

  get conFuncion(): boolean {
    return !!this.resumen?.idShowtime;
  }

  get totalTickets(): number {
    return this.resumen?.tickets?.reduce((sum, t) => sum + t.subtotal, 0) ?? 0;
  }

  get totalSnacks(): number {
    return this.resumen?.snacks?.reduce((sum, s) => sum + s.subtotal, 0) ?? 0;
  }

  get asientosTexto(): string {
    const lista = this.resumen?.asientos;
    return lista?.length ? lista.join(', ') : 'Por elegir';
  }

  // "2026-11-13 19:30:00" → "vie 13 nov · 19:30"
  get funcionTexto(): string {
    const [fecha, hora] = (this.resumen?.fechaHora ?? '').split(' ');
    if (!fecha) return '';
    const dia = new Date(`${fecha}T00:00:00`);
    const texto = isNaN(dia.getTime()) ? fecha
      : dia.toLocaleDateString('es-PE', { weekday: 'short', day: 'numeric', month: 'short' }).replace(/[.,]/g, '');
    return hora ? `${texto} · ${hora.slice(0, 5)}` : texto;
  }
}
