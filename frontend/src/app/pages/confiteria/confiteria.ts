import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { AuthService } from '../../services/auth.service';
import { BookingResumen, BookingService, SnackEntry } from '../../services/booking';
import { Offcanvas } from 'bootstrap';
import { TarjetaSnack } from '../../shared/components/tarjeta-snack/tarjeta-snack';
import { BookingSummaryComponent } from '../checkout/shared/booking-summary/booking-summary';

@Component({
  selector: 'app-confiteria',
  standalone: true,
  imports: [TarjetaSnack, BookingSummaryComponent],
  templateUrl: './confiteria.html',
  styleUrl: './confiteria.css'
})
export class Confiteria implements OnInit {

  // --- Estado de datos ---
  snacks: any[] = [];
  categories: any[] = [];
  sedes: any[] = [];
  sedeSeleccionada: any = null;
  categoriaActiva: any = null;
  carrito: { snack: any; cantidad: number }[] = [];

  // --- Estado de UI ---
  cargando = false;
  cargandoSedes = true;
  error = false;

  constructor(
    private cdr: ChangeDetectorRef,
    private http: HttpClient,
    private authService: AuthService,
    private bookingService: BookingService,
    private router: Router
  ) {}

  // --- Inicialización ---

  ngOnInit(): void {
    this.cargarSedes();
  }

  // --- Carga de sedes públicas ---

  cargarSedes(): void {
    this.cargandoSedes = true;
    this.error = false;
    this.http.get<any[]>(`${environment.apiUrl}/venues/public`).subscribe({
      next: (res) => {
        this.sedes = res;
        this.cargandoSedes = false;
        // Abre el cine del pedido en curso (si vuelve del pago) o el elegido en la cabecera.
        const pedido = this.pedidoEnCurso();
        const guardado = pedido?.idVenue ?? Number(localStorage.getItem('cinemax_cine'));
        const sede = res.find(s => s.idVenue === guardado);
        if (sede) this.seleccionarSede(sede);
        else this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoSedes = false;
        this.error = true;
        this.cdr.detectChanges();
      }
    });
  }

  // --- Selección y cambio de sede ---

  elegirSede(valor: string): void {
    const sede = this.sedes.find(s => s.idVenue === Number(valor));
    if (sede) this.seleccionarSede(sede);
    else this.cambiarSede();
  }

  // El stock es de cada cine: al cambiar de cine se vacía el pedido.
  seleccionarSede(sede: any): void {
    if (this.sedeSeleccionada?.idVenue !== sede.idVenue) this.carrito = [];
    this.sedeSeleccionada = sede;
    this.cargando = true;
    this.error = false;
    this.cdr.detectChanges();
    this.cargarDatos();
  }

  cambiarSede(): void {
    this.sedeSeleccionada = null;
    this.carrito = [];
    this.snacks = [];
    this.categoriaActiva = null;
    this.cargando = false;
    this.error = false;
    this.cdr.detectChanges();
  }

  // --- Carga paralela de categorías y snacks de la sede ---

  cargarDatos(): void {
    this.cargando = true;
    this.error = false;
    let categoriasOk = false;
    let snacksOk = false;

    // Ambas llamadas corren en paralelo; cargando se apaga cuando las dos terminan
    const verificarFin = () => {
      if (categoriasOk && snacksOk) {
        this.cargando = false;
        this.error = false;
        this.cdr.detectChanges();
      }
    };

    this.http.get<any[]>(`${environment.apiUrl}/snack-categories`).subscribe({
      next: (cats: any[]) => {
        this.categories = cats;
        if (cats.length) this.categoriaActiva = cats[0]; // activa la primera categoría por defecto
        categoriasOk = true;
        verificarFin();
      },
      error: () => {
        this.cargando = false;
        this.error = true;
        this.cdr.detectChanges();
      }
    });

    this.http.get<any[]>(`${environment.apiUrl}/snacks/venue/${this.sedeSeleccionada.idVenue}`).subscribe({
      next: (res: any[]) => {
        this.snacks = res.filter((s: any) => s.status === 'Activo'); // solo snacks activos
        this.recuperarPedido();
        snacksOk = true;
        verificarFin();
      },
      error: () => {
        this.cargando = false;
        this.error = true;
        this.cdr.detectChanges();
      }
    });
  }

  private pedidoEnCurso(): BookingResumen | null {
    const r = this.bookingService.obtenerResumen();
    return !r.idShowtime && r.idVenue && r.snacks?.length ? r : null;
  }

  private recuperarPedido(): void {
    const pedido = this.pedidoEnCurso();
    if (!pedido || pedido.idVenue !== this.sedeSeleccionada?.idVenue || this.carrito.length) return;
    for (const linea of pedido.snacks) {
      const snack = this.snacks.find(s => s.idSnack === linea.idSnack);
      if (snack?.stock > 0) this.carrito.push({ snack, cantidad: Math.min(linea.cantidad, snack.stock) });
    }
  }

  // --- Filtrado por categoría activa ---

  get snacksFiltrados(): any[] {
    if (!this.categoriaActiva) return this.snacks;
    return this.snacks.filter(s => s.idSnackCategory === this.categoriaActiva.idSnackCategory);
  }

  // --- Manejo del carrito ---

  agregarAlCarrito(snack: any): void {
  const item = this.carrito.find(c => c.snack.idSnack === snack.idSnack);
  if (item) {
    if (item.cantidad >= snack.stock) return; // no superar el stock
    item.cantidad++;
  } else {
    this.carrito.push({ snack, cantidad: 1 });
  }
}

  quitarDelCarrito(snack: any): void {
    const idx = this.carrito.findIndex(c => c.snack.idSnack === snack.idSnack);
    if (idx === -1) return;
    if (this.carrito[idx].cantidad > 1) {
      this.carrito[idx].cantidad--;
    } else {
      this.carrito.splice(idx, 1); // elimina el item si llega a 0
    }
  }

  cantidadEnCarrito(snack: any): number {
    return this.carrito.find(c => c.snack.idSnack === snack.idSnack)?.cantidad ?? 0;
  }

  // --- Resumen lateral ---

  private lineasPedido(): SnackEntry[] {
    return this.carrito.map(item => ({
      idSnack:  item.snack.idSnack,
      nombre:   item.snack.nameSnack,
      precio:   item.snack.price,
      cantidad: item.cantidad,
      subtotal: item.snack.price * item.cantidad,
      imagen:   item.snack.imageUrlSnack
    }));
  }

  get resumen(): BookingResumen {
    const snacks = this.lineasPedido();
    const total = snacks.reduce((t, s) => t + s.subtotal, 0);
    return {
      idShowtime: null, pelicula: { nombre: '', formato: '', poster: '' },
      cine: this.sedeSeleccionada?.nameVenue ?? '', sala: '', idVenue: this.sedeSeleccionada?.idVenue ?? null,
      fechaHora: '', asientos: [], asientosIds: [], tickets: [], totalTickets: 0,
      snacks, totalSnacks: total, totalPagar: total
    };
  }

  // --- Flujo de compra ---

  continuarCompra(): void {
    // Si no está logueado, abre el panel de login
    if (!this.authService.isLoggedIn()) {
      const offcanvasEl = document.getElementById('authOffcanvas');
      if (offcanvasEl) {
        const offcanvas = Offcanvas.getOrCreateInstance(offcanvasEl);
        offcanvas.show();
      }
      return;
    }

    // Limpia cualquier reserva previa y guarda solo los snacks, luego va al pago
    this.bookingService.limpiar();
    this.bookingService.guardarSede(this.sedeSeleccionada.idVenue, this.sedeSeleccionada.nameVenue);
    this.bookingService.guardarSnacks(this.lineasPedido());
    this.router.navigate(['/payment']);
  }
}