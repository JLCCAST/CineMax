import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Promotion, PromotionService } from '../../../services/promotion.service';
import { environment } from '../../../environments/environment';

interface Categoria {
  idSnackCategory: number;
  nameCategory: string;
}

const DIAS = [
  { n: 1, nombre: 'Lun' }, { n: 2, nombre: 'Mar' }, { n: 3, nombre: 'Mié' }, { n: 4, nombre: 'Jue' },
  { n: 5, nombre: 'Vie' }, { n: 6, nombre: 'Sáb' }, { n: 7, nombre: 'Dom' }
];

@Component({
  selector: 'app-admin-promotions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './promotions.html',
  styleUrl: './promotions.css'
})
export class AdminPromotions implements OnInit {
  private readonly service = inject(PromotionService);
  private readonly http = inject(HttpClient);

  readonly dias = DIAS;
  readonly promociones = signal<Promotion[]>([]);
  readonly tipos = signal<string[]>([]);
  readonly categorias = signal<Categoria[]>([]);
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly mensaje = signal('');
  readonly error = signal('');
  readonly formularioAbierto = signal(false);

  // Lo edita el formulario con ngModel; se copia al abrirlo.
  form: Promotion = this.vacia();

  ngOnInit(): void {
    this.cargar();
    this.service.types().subscribe(tipos => this.tipos.set(tipos));
    this.http.get<Categoria[]>(`${environment.apiUrl}/snack-categories`)
      .subscribe(categorias => this.categorias.set(categorias));
  }

  cargar(): void {
    this.service.list().subscribe({
      next: promociones => {
        this.promociones.set(promociones);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las promociones.');
        this.cargando.set(false);
      }
    });
  }

  nueva(): void {
    this.form = this.vacia();
    this.abrir();
  }

  editar(p: Promotion): void {
    this.form = { ...p };
    this.abrir();
  }

  cancelar(): void {
    this.formularioAbierto.set(false);
  }

  diaMarcado(n: number): boolean {
    return (this.form.dayOfWeek ?? '').split(',').includes(String(n));
  }

  alternarDia(n: number): void {
    const dias = new Set((this.form.dayOfWeek ?? '').split(',').filter(d => d));
    dias.has(String(n)) ? dias.delete(String(n)) : dias.add(String(n));
    this.form.dayOfWeek = dias.size ? [...dias].sort().join(',') : null;
  }

  esCumpleanos(): boolean {
    return this.form.customerCondition === 'CUMPLEANOS';
  }

  alternarCumpleanos(activo: boolean): void {
    this.form.customerCondition = activo ? 'CUMPLEANOS' : null;
  }

  guardar(): void {
    this.guardando.set(true);
    this.error.set('');
    const cuerpo = { ...this.form, promotionCode: this.form.promotionCode?.trim() || null };
    const op = this.form.idPromotion
      ? this.service.update(this.form.idPromotion, cuerpo)
      : this.service.create(cuerpo);

    op.subscribe({
      next: () => {
        this.guardando.set(false);
        this.formularioAbierto.set(false);
        this.mensaje.set('Promoción guardada.');
        this.cargar();
      },
      error: err => {
        this.guardando.set(false);
        this.error.set(err.error?.message ?? 'No se pudo guardar la promoción.');
      }
    });
  }

  alternarEstado(p: Promotion): void {
    const nuevo = p.status === 'Activo' ? 'Inactivo' : 'Activo';
    this.service.setStatus(p.idPromotion!, nuevo).subscribe({
      next: () => {
        this.mensaje.set(`"${p.namePromotion}" quedó ${nuevo.toLowerCase()}.`);
        this.cargar();
      },
      error: err => this.error.set(err.error?.message ?? 'No se pudo cambiar el estado.')
    });
  }

  beneficio(p: Promotion): string {
    const tope = p.maxDiscountAmount ? ` (tope S/ ${p.maxDiscountAmount})` : '';
    switch (p.promotionType) {
      case 'PORCENTAJE': return `${p.discountPercentage}%${tope}`;
      case 'MONTO_FIJO': return `S/ ${p.discountFixedAmount}${tope}`;
      case 'DOS_POR_UNO': return '2x1';
      case 'ENTRADA_GRATIS': return `1 entrada gratis${tope}`;
      default: return p.promotionType;
    }
  }

  condiciones(p: Promotion): string[] {
    const c: string[] = [];
    if (p.promotionCode) c.push(`Cupón ${p.promotionCode}`);
    if (p.dayOfWeek) c.push(p.dayOfWeek.split(',').map(d => DIAS[+d - 1].nombre).join(', '));
    if (p.startTimeFrom) c.push(`Desde ${p.startTimeFrom.slice(0, 5)}`);
    if (p.startTimeTo) c.push(`Antes de ${p.startTimeTo.slice(0, 5)}`);
    if (p.excludePremiereMovies) c.push('Sin estrenos');
    if (p.minimumTicketQuantity && p.minimumTicketQuantity > 1) c.push(`${p.minimumTicketQuantity}+ entradas`);
    if (p.minimumPurchaseAmount && p.minimumPurchaseAmount > 0) c.push(`Compra desde S/ ${p.minimumPurchaseAmount}`);
    if (p.requiredSnackCategoryId) {
      const categoria = this.categorias().find(x => x.idSnackCategory === p.requiredSnackCategoryId);
      c.push(`Con ${categoria?.nameCategory ?? 'categoría ' + p.requiredSnackCategoryId}`);
    }
    if (p.customerCondition === 'CUMPLEANOS') c.push('Cumpleaños del cliente');
    if (p.maxUsesPerUser) c.push(`${p.maxUsesPerUser} uso(s) al año`);
    return c;
  }

  private abrir(): void {
    this.error.set('');
    this.mensaje.set('');
    this.formularioAbierto.set(true);
  }

  private vacia(): Promotion {
    const hoy = new Date().toISOString().slice(0, 10);
    return {
      namePromotion: '', promotionType: 'PORCENTAJE', promotionCode: null,
      discountPercentage: null, discountFixedAmount: null, maxDiscountAmount: null,
      minimumPurchaseAmount: 0, minimumTicketQuantity: 1,
      startDate: hoy, endDate: hoy, dayOfWeek: null, startTimeFrom: null, startTimeTo: null,
      excludePremiereMovies: false, requiredSnackCategoryId: null, customerCondition: null,
      maxUsesPerUser: null, appliesTo: 'ENTRADAS', priority: 0, status: 'Activo',
      description: null, imageUrlPromotion: null
    };
  }
}
