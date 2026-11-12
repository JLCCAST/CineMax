import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { BookingService } from '../../../services/booking';
import { AuthService } from '../../../services/auth.service';
import { SaleTransactionService, SaleTransactionRequestDTO, SaleTransactionResponseDTO } from '../../../services/sale-transaction.service';
import { environment } from '../../../environments/environment';

type MetodoPago = 'tarjeta' | 'yape';

interface CardForm {
  titular: string;
  numero: string;
  vencimiento: string;
  cvv: string;
}

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './payment.html',
  styleUrl: './payment.css'
})
export class Payment implements OnInit {

  resumen: any;
  metodoPago: MetodoPago = 'tarjeta';

  card: CardForm = { titular: '', numero: '', vencimiento: '', cvv: '' };
  yapenumero = '';

  // Código promocional
  codigoPromo    = '';
  promoAplicada: any = null;
  promoError     = '';
  cargandoPromo  = false;

  // Estado del pago
  procesando   = false;
  errorPago    = '';

  // Resultado exitoso — guardamos la respuesta del backend
  pagoExitoso       = false;
  transaccionRespuesta: SaleTransactionResponseDTO | null = null;
  resumenSnapshot: any = null;

  // Número de tarjeta formateado para la vista previa
  numeroMostrado = '';

  private readonly apiUrl = environment.apiUrl;

  constructor(
    public router: Router,
    private bookingService: BookingService,
    private authService: AuthService,
    private saleTransactionService: SaleTransactionService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

ngOnInit(): void {
  this.resumen = this.bookingService.obtenerResumen();
  if (!this.resumen.tickets?.length && !this.resumen.snacks?.length) {
    this.router.navigate(['/seats']);
  }
}

  // ── Método de pago ────────────────────────────────────────────────────────
  seleccionarMetodo(m: MetodoPago): void {
    this.metodoPago = m;
    this.errorPago  = '';
  }

  // ── Formateo de tarjeta ───────────────────────────────────────────────────
  onNumeroInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 16);
    this.card.numero    = digits;
    this.numeroMostrado = digits.replace(/(.{4})/g, '$1 ').trim();
    input.value         = this.numeroMostrado;
  }

 onVencimientoInput(event: Event): void {
  const input = event.target as HTMLInputElement;
  let digits = input.value.replace(/\D/g, '').slice(0, 4);

  // Validación de que el primer dígito no sea > 1
  if (digits.length >= 1 && parseInt(digits[0]) > 1) {
    digits = '';
  }

  // Validación de que los primeros dos dígitos sean mes válido (01-12)
  if (digits.length >= 2) {
    const mes = parseInt(digits.slice(0, 2));
    if (mes < 1 || mes > 12) {
      digits = digits.slice(0, 1); 
    }
  }

  // Formatear con /
  let val = digits;
  if (val.length >= 3) val = val.slice(0, 2) + '/' + val.slice(2);

  this.card.vencimiento = val;
  input.value = val;
}

  get tipoTarjeta(): string {
    const n = this.card.numero;
    if (n.startsWith('4'))                         return 'visa';
    if (n.startsWith('5') || n.startsWith('2'))    return 'mastercard';
    if (n.startsWith('34') || n.startsWith('37'))  return 'amex';
    return '';
  }

  // ── Código promo ──────────────────────────────────────────────────────────
  aplicarPromo(): void {
    if (!this.codigoPromo.trim()) return;
    this.promoError   = '';
    this.cargandoPromo = true;

    this.http.post<any>(`${this.apiUrl}/promotions/calculate`, {
      idShowtime:    this.resumen.idShowtime,
      subtotal:      this.totalSinDescuento,
      promotionCode: this.codigoPromo.trim().toUpperCase()
    }).subscribe({
      next: (res) => {
        this.promoAplicada = res;
        this.cargandoPromo = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.promoError    = typeof err.error === 'string' ? err.error : (err.error?.message ?? 'Código inválido.');
        this.promoAplicada = null;
        this.cargandoPromo = false;
        this.cdr.detectChanges();
      }
    });
  }

  quitarPromo(): void {
    this.promoAplicada = null;
    this.codigoPromo   = '';
    this.promoError    = '';
  }

  // ── Totales ───────────────────────────────────────────────────────────────
  get totalSinDescuento(): number {
    return this.bookingService.calcularTotalTickets() + this.bookingService.calcularTotalSnacks();
  }

  get descuento(): number {
    return this.promoAplicada ? Number(this.promoAplicada.discountAmount) : 0;
  }

  get totalFinal(): number {
    return this.promoAplicada ? Number(this.promoAplicada.finalAmount) : this.totalSinDescuento;
  }

  // ── Validación ────────────────────────────────────────────────────────────
  get tarjetaVencida(): boolean {
    if (!/^\d{2}\/\d{2}$/.test(this.card.vencimiento)) return false;
    const [mes, anio] = this.card.vencimiento.split('/').map(Number);
    const anioCompleto = 2000 + anio;
    const ahora = new Date();
    const vencimiento = new Date(anioCompleto, mes, 1);
    return vencimiento <= ahora;
  }

  get mesInvalido(): boolean {
    const digits = this.card.vencimiento.replace(/\D/g, '');
    if (digits.length < 2) return false;
    const mes = parseInt(digits.slice(0, 2));
    return mes < 1 || mes > 12;
  }

  get formularioValido(): boolean {
    if (this.metodoPago === 'tarjeta') {
      return (
        this.card.titular.trim().length >= 3 &&
        this.card.numero.length === 16 &&
        /^\d{2}\/\d{2}$/.test(this.card.vencimiento) &&
        !this.tarjetaVencida &&
        this.card.cvv.length >= 3
      );
    }
    if (this.metodoPago === 'yape') {
      return /^\d{9}$/.test(this.yapenumero);
    }
    return false;
  }

  // ── Confirmar pago ────────────────────────────────────────────────────────
  confirmarPago(): void {
    if (!this.formularioValido || this.procesando) return;

    this.procesando = true;
    this.errorPago  = '';
    this.cdr.detectChanges();

    this.resumenSnapshot = JSON.parse(JSON.stringify(this.resumen));

    const request: SaleTransactionRequestDTO = {
      idShowtime:     this.resumen.idShowtime,
      asientosIds:    this.resumen.asientosIds,

      tickets: this.resumen.tickets.map((t: any) => ({
        categoryCode:   t.categoryCode,
        cantidad:       t.cantidad,
        precioUnitario: t.precioUnitario
      })),

      snacks: (this.resumen.snacks ?? []).map((s: any) => ({
        idSnack:   s.idSnack,
        cantidad:  s.cantidad,
        unitPrice: s.precio
      })),

      subtotal:       this.totalSinDescuento,
      discountAmount: this.descuento,
      totalAmount:    this.totalFinal,
      paymentMethod:  this.metodoPago.toUpperCase() as 'TARJETA' | 'YAPE',
      idPromotion:    this.promoAplicada?.idPromotion ?? null
    };

    this.saleTransactionService.createSaleTransaction(request).subscribe({
      next: (res: SaleTransactionResponseDTO) => {
        this.transaccionRespuesta = res;
        this.procesando           = false;
        this.pagoExitoso          = true;
        this.bookingService.limpiar();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.procesando = false;
        const msg = typeof err.error === 'string'
          ? err.error
          : (err.error?.message ?? 'Ocurrió un error al procesar el pago. Intenta de nuevo.');
        this.errorPago = msg;
        this.cdr.detectChanges();
      }
    });
  }

  // ── Helpers de la pantalla de éxito ──────────────────────────────────────
  get qrImageUrl(): string {
    const data = this.transaccionRespuesta?.qrCodeData ?? '';
    return `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(data)}`;
  }

  descargarTicket(): void {
    window.open(this.qrImageUrl + '&format=png', '_blank');
  }

  qrFallback(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
  }

  volverAlInicio(): void {
    this.router.navigate(['/movies']);
  }
}