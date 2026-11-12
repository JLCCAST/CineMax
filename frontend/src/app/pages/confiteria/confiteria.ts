import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { ConfiteriaService } from '../../services/confiteria.service';
import { environment } from '../../environments/environment';
import { AuthService } from '../../services/auth.service';
import { BookingService, SnackEntry } from '../../services/booking';
import { Offcanvas } from 'bootstrap';

@Component({
  selector: 'app-confiteria',
  standalone: true,
  imports: [CommonModule, FormsModule],
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
    private snackService: ConfiteriaService,
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
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoSedes = false;
        this.error = true;
        this.cdr.detectChanges();
      }
    });
  }

  // --- Selección y cambio de sede ---

  seleccionarSede(sede: any): void {
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

  // --- Totales del carrito ---

  get totalCarrito(): number {
    return this.carrito.reduce((acc, c) => acc + c.snack.price * c.cantidad, 0);
  }

  get totalItems(): number {
    return this.carrito.reduce((acc, c) => acc + c.cantidad, 0);
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

    // Mapea el carrito al formato que espera BookingService
    const snackEntries: SnackEntry[] = this.carrito.map(item => ({
      idSnack:  item.snack.idSnack,
      nombre:   item.snack.nameSnack,
      precio:   item.snack.price,
      cantidad: item.cantidad,
      subtotal: item.snack.price * item.cantidad,
      imagen:   item.snack.imageUrlSnack
    }));

    // Limpia cualquier reserva previa y guarda solo los snacks, luego va al pago
    this.bookingService.limpiar();
    this.bookingService.guardarSede(this.sedeSeleccionada.idVenue, this.sedeSeleccionada.nameVenue);
    this.bookingService.guardarSnacks(snackEntries);
    this.router.navigate(['/payment']);
  }
}