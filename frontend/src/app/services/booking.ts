import { Injectable } from '@angular/core';

export interface TicketEntry {
  categoryCode: string;
  categoryName: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface SnackEntry {
  idSnack: number;
  nombre: string;
  precio: number;
  cantidad: number;
  subtotal: number;
  imagen?: string;
}

export interface BookingResumen {
  idShowtime: string | null;
  pelicula: { nombre: string; formato: string; poster: string };
  cine: string;
  sala: string;
  idVenue: number | null;
  fechaHora: string;
  asientos: string[];
  asientosIds: number[];
  tickets: TicketEntry[];
  totalTickets: number;
  snacks: SnackEntry[];
  totalSnacks: number;
  totalPagar: number;
}

@Injectable({ providedIn: 'root' })
export class BookingService {

  private readonly STORAGE_KEY = 'cinemax_booking';

  private reserva: BookingResumen = this.reservaVacia();

  constructor() {
    const guardado = sessionStorage.getItem(this.STORAGE_KEY);
    if (guardado) {
      try { this.reserva = JSON.parse(guardado); } catch { }
    }
  }

  iniciarReserva(idShowtime: string): void {
    this.reserva = { ...this.reservaVacia(), idShowtime };
    this.persistir();
  }

  limpiar(): void {
    this.reserva = this.reservaVacia();
    sessionStorage.removeItem(this.STORAGE_KEY);
  }

  obtenerResumen(): BookingResumen {
    return this.reserva;
  }

  get idShowtime(): string | null {
    return this.reserva.idShowtime;
  }

  // ── Paso 1: Asientos ─────────────────────────────────────────────────────
  guardarAsientos(codigos: string[], ids: number[]): void {
    this.reserva.asientos    = [...codigos];
    this.reserva.asientosIds = [...ids];
    this.persistir();
  }

  /**
   * Guarda los asientos y limpia tickets + snacks (porque cambiaron los asientos).
   * Llamar cuando el usuario confirma selección desde seats.
   */
  guardarAsientosYLimpiarSiguientes(codigos: string[], ids: number[]): void {
    this.reserva.asientos    = [...codigos];
    this.reserva.asientosIds = [...ids];
    this.reserva.tickets     = [];
    this.reserva.totalTickets = 0;
    this.reserva.snacks      = [];
    this.reserva.totalSnacks = 0;
    this.reserva.totalPagar  = 0;
    this.persistir();
  }

  // ── Paso 2: Info película ────────────────────────────────────────────────
  guardarInfoShowtime(data: {
    nombre: string; formato: string; poster: string;
    cine: string; sala: string; idVenue: number | null; fechaHora: string;
  }): void {
    this.reserva.pelicula  = { nombre: data.nombre, formato: data.formato, poster: data.poster };
    this.reserva.cine      = data.cine;
    this.reserva.sala      = data.sala;
    this.reserva.idVenue   = data.idVenue;
    this.reserva.fechaHora = data.fechaHora;
    this.persistir();
  }

  // Pedido solo de confitería: no hay función, solo la sede donde se recoge.
  guardarSede(idVenue: number, cine: string): void {
    this.reserva.idVenue = idVenue;
    this.reserva.cine    = cine;
    this.persistir();
  }

  // ── Paso 3: Tickets ───────────────────────────────────────────────────────
  guardarTickets(tickets: TicketEntry[]): void {
    this.reserva.tickets      = tickets.filter(t => t.cantidad > 0);
    this.reserva.totalTickets = this.reserva.tickets.reduce((s, t) => s + t.subtotal, 0);
    this.reserva.totalPagar   = this.reserva.totalTickets + this.reserva.totalSnacks;
    this.persistir();
  }

  calcularTotalTickets(): number {
    return this.reserva.tickets.reduce((s, t) => s + t.subtotal, 0);
  }

  // ── Paso 4: Snacks ────────────────────────────────────────────────────────
  guardarSnacks(snacks: SnackEntry[]): void {
    this.reserva.snacks      = snacks;
    this.reserva.totalSnacks = snacks.reduce((t, s) => t + s.subtotal, 0);
    this.reserva.totalPagar  = this.calcularTotalTickets() + this.reserva.totalSnacks;
    this.persistir();
  }

  calcularTotalSnacks(): number {
    return this.reserva.snacks.reduce((t, s) => t + s.subtotal, 0);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  private reservaVacia(): BookingResumen {
    return {
      idShowtime: null,
      pelicula: { nombre: '', formato: '', poster: '' },
      cine: '', sala: '', idVenue: null, fechaHora: '',
      asientos: [], asientosIds: [],
      tickets: [], totalTickets: 0,
      snacks: [], totalSnacks: 0,
      totalPagar: 0
    };
  }

  private persistir(): void {
    sessionStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.reserva));
  }
}