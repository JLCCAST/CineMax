import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { BookingService, SnackEntry } from '../../../services/booking';
import { environment } from '../../../environments/environment';

interface SnackItem {
  idSnack: number;
  nameSnack: string;
  descriptionSnack: string;
  price: number;
  imageUrlSnack: string;
  idSnackCategory: number;
  nameCategory: string;
  stock: number;
  cantidad: number;
}

interface Category {
  idSnackCategory: number;
  nameCategory: string;
}

/**
 * Icono por nombre de categoría (case-insensitive, parcial).
 * El icono de bebidas antes usaba fa-cup-straw pero es de FA Pro;
 * se reemplaza por fa-martini-glass-citrus (free) o fa-wine-glass (free).
 */
function resolveIcon(nombre: string): string {
  const n = nombre.toLowerCase();
  if (n.includes('bebida') || n.includes('drink') || n.includes('refresco') || n.includes('jugo'))
    return 'fa-solid fa-wine-glass';
  if (n.includes('combos') || n.includes('combo'))
    return 'fa-solid fa-box-open';
  if (n.includes('comida') || n.includes('hot dog') || n.includes('nachos') || n.includes('salado'))
    return 'fa-solid fa-bowl-food';
  if (n.includes('dulce') || n.includes('golosina') || n.includes('chocolate'))
    return 'fa-solid fa-candy-cane';
  if (n.includes('galleta') || n.includes('cookie') || n.includes('snack'))
    return 'fa-solid fa-cookie-bite';
  if (n.includes('pop') || n.includes('crispeta') || n.includes('canchita') || n.includes('maíz'))
    return 'fa-solid fa-popcorn';
  return 'fa-solid fa-utensils';   // fallback genérico
}

@Component({
  selector: 'app-snacks',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './snacks.html',
  styleUrl: './snacks.css'
})
export class Snacks implements OnInit {

  resumen: any;
  categorias: Category[] = [];
  snacks: SnackItem[] = [];
  categoriaActiva: number = 1;

  // Paginación por categoría
  paginasPorCategoria: Record<number, number> = {};
  readonly POR_PAGINA = 6;

  cargando = true;
  error = '';

  private readonly apiUrl = environment.apiUrl;

  constructor(
    private router: Router,
    private bookingService: BookingService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.resumen = this.bookingService.obtenerResumen();

    if (!this.resumen.tickets?.length) {
      this.router.navigate(['/tickets']);
      return;
    }

    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando = true;

    this.http.get<Category[]>(`${this.apiUrl}/snack-categories`).subscribe({
      next: (cats) => {
        this.categorias = cats.sort((a, b) => a.idSnackCategory - b.idSnackCategory);
        if (this.categorias.length) {
          this.categoriaActiva = this.categorias[0].idSnackCategory;
          this.categorias.forEach(c => this.paginasPorCategoria[c.idSnackCategory] = 0);
        }
        this.cdr.detectChanges();
      }
    });

    // Solo lo que vende la sede de la función: la compra descuenta el stock de esa sede.
    const url = this.resumen.idVenue
      ? `${this.apiUrl}/snacks/venue/${this.resumen.idVenue}`
      : `${this.apiUrl}/snacks`;
    this.http.get<any[]>(url).subscribe({
      next: (res) => {
        this.snacks = res
          .filter(s => s.status === 'Activo')
          .map(s => ({
            idSnack:          s.idSnack,
            nameSnack:        s.nameSnack,
            descriptionSnack: s.descriptionSnack,
            price:            Number(s.price),
            imageUrlSnack:    s.imageUrlSnack || '',
            idSnackCategory:  s.idSnackCategory,
            nameCategory:     s.nameCategory,
            stock:            s.stock,
            cantidad:         0
          }));

        // Restaurar snacks previos si el usuario regresa
        const previos = this.resumen.snacks as SnackEntry[];
        if (previos?.length) {
          previos.forEach(prev => {
            const item = this.snacks.find(s => s.idSnack === prev.idSnack);
            if (item) item.cantidad = prev.cantidad;
          });
        }

        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo cargar la confitería.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  // ── Filtrado y paginación ─────────────────────────────────────────────────

  get snacksDeCategoriaActiva(): SnackItem[] {
    return this.snacks.filter(s => s.idSnackCategory === this.categoriaActiva);
  }

  get paginaActual(): number {
    return this.paginasPorCategoria[this.categoriaActiva] ?? 0;
  }

  get totalPaginas(): number {
    return Math.ceil(this.snacksDeCategoriaActiva.length / this.POR_PAGINA);
  }

  get snacksPagina(): SnackItem[] {
    const inicio = this.paginaActual * this.POR_PAGINA;
    return this.snacksDeCategoriaActiva.slice(inicio, inicio + this.POR_PAGINA);
  }

  get paginasArray(): number[] {
    return Array.from({ length: this.totalPaginas }, (_, i) => i);
  }

  irPagina(n: number): void {
    if (n < 0 || n >= this.totalPaginas) return;
    this.paginasPorCategoria[this.categoriaActiva] = n;
  }

  setCategoriaActiva(id: number): void {
    this.categoriaActiva = id;
    // Resetear paginación al cambiar de categoría
    if (this.paginasPorCategoria[id] === undefined) {
      this.paginasPorCategoria[id] = 0;
    }
  }

  /** Resuelve el icono a partir del nombre de la categoría (no del id hardcodeado) */
  iconCategoria(idCategoria: number): string {
    const cat = this.categorias.find(c => c.idSnackCategory === idCategoria);
    return cat ? resolveIcon(cat.nameCategory) : 'fa-solid fa-utensils';
  }

  // ── Carrito ───────────────────────────────────────────────────────────────

  sumar(snack: SnackItem): void {
    if (snack.cantidad < snack.stock) snack.cantidad++;
  }

  restar(snack: SnackItem): void {
    if (snack.cantidad > 0) snack.cantidad--;
  }

  get carrito(): SnackItem[] {
    return this.snacks.filter(s => s.cantidad > 0);
  }

  get totalItems(): number {
    return this.snacks.reduce((t, s) => t + s.cantidad, 0);
  }

  get totalSnacks(): number {
    return this.snacks.reduce((t, s) => t + s.price * s.cantidad, 0);
  }

  get totalTickets(): number {
    return this.bookingService.calcularTotalTickets();
  }

  get totalGeneral(): number {
    return this.totalTickets + this.totalSnacks;
  }

  // ── Navegación ────────────────────────────────────────────────────────────

  continuarAlPago(): void {
    const seleccionados: SnackEntry[] = this.carrito.map(s => ({
      idSnack:  s.idSnack,
      nombre:   s.nameSnack,
      precio:   s.price,
      cantidad: s.cantidad,
      subtotal: s.price * s.cantidad,
      imagen:   s.imageUrlSnack
    }));
    this.bookingService.guardarSnacks(seleccionados);
    this.router.navigate(['/payment']);
  }

  saltarSnacks(): void {
    this.bookingService.guardarSnacks([]);
    this.router.navigate(['/payment']);
  }

  regresar(): void {
    this.router.navigate(['/tickets']);
  }
}