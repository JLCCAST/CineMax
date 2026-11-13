import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { BookingResumen, BookingService } from '../../../services/booking';
import { AuthService } from '../../../services/auth.service';
import { SaleTransactionService, SaleTransactionRequestDTO, SaleTransactionResponseDTO } from '../../../services/sale-transaction.service';
import { environment } from '../../../environments/environment';
import { toDataURL } from 'qrcode';
import { BookingSummaryComponent } from '../shared/booking-summary/booking-summary';
import { CheckoutStepperComponent } from '../shared/checkout-stepper/checkout-stepper';

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
  imports: [DecimalPipe, FormsModule, BookingSummaryComponent, CheckoutStepperComponent],
  templateUrl: './payment.html',
  styleUrl: './payment.css'
})
export class Payment implements OnInit {

  resumen!: BookingResumen;
  metodoPago: MetodoPago = 'tarjeta';

  card: CardForm = { titular: '', numero: '', vencimiento: '', cvv: '' };
  yapenumero = '';

  // Cotización del servidor: precios, promoción aplicada y total
  cotizacion: any = null;

  // Código promocional
  codigoPromo    = '';
  cuponAplicado: string | null = null;
  promoError     = '';
  cargandoPromo  = false;

  // Estado del pago
  procesando   = false;
  errorPago    = '';

  // Resultado exitoso — guardamos la respuesta del backend
  pagoExitoso       = false;
  transaccionRespuesta: SaleTransactionResponseDTO | null = null;
  resumenSnapshot: BookingResumen | null = null;

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
      return;
    }
    this.cotizar(null);
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

  // ── Cotización y código promo ──────────────────────────────────────────────
  // La promoción la decide el servidor; el cupón es una condición más.
  private cotizar(cupon: string | null, alTerminar?: (res: any) => void): void {
    this.http.post<any>(`${this.apiUrl}/promotions/preview`, {
      idShowtime:    this.resumen.idShowtime,
      idVenue:       this.resumen.idVenue,
      tickets:       this.resumen.tickets.map((t: any) => ({ categoryCode: t.categoryCode, cantidad: t.cantidad })),
      snacks:        (this.resumen.snacks ?? []).map((s: any) => ({ idSnack: s.idSnack, cantidad: s.cantidad })),
      promotionCode: cupon
    }).subscribe({
      next: (res) => {
        this.cotizacion = res;
        alTerminar?.(res);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorPago = err.error?.message ?? 'No se pudo calcular el total. Vuelve a intentarlo.';
        this.cargandoPromo = false;
        this.cdr.detectChanges();
      }
    });
  }

  aplicarPromo(): void {
    const codigo = this.codigoPromo.trim().toUpperCase();
    if (!codigo) return;
    this.promoError    = '';
    this.cargandoPromo = true;

    this.cotizar(codigo, (res) => {
      this.cargandoPromo = false;
      if (res.promotionCode?.toUpperCase() === codigo) {
        this.cuponAplicado = codigo;
      } else {
        this.promoError = 'El cupón no aplica a esta compra.';
        this.cotizar(null);
      }
    });
  }

  quitarPromo(): void {
    this.cuponAplicado = null;
    this.codigoPromo   = '';
    this.promoError    = '';
    this.cotizar(null);
  }

  // ── Totales (del servidor) ─────────────────────────────────────────────────
  get promoAplicada(): any {
    return this.cotizacion?.namePromotion ? this.cotizacion : null;
  }

  get totalSinDescuento(): number {
    return this.cotizacion
      ? Number(this.cotizacion.subtotal)
      : this.bookingService.calcularTotalTickets() + this.bookingService.calcularTotalSnacks();
  }

  get descuento(): number {
    return this.cotizacion ? Number(this.cotizacion.discountAmount) : 0;
  }

  get totalFinal(): number {
    return this.cotizacion ? Number(this.cotizacion.totalAmount) : this.totalSinDescuento;
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
    if (!this.formularioValido || this.procesando || !this.cotizacion) return;

    this.procesando = true;
    this.errorPago  = '';
    this.cdr.detectChanges();

    this.resumenSnapshot = JSON.parse(JSON.stringify(this.resumen));

    const request: SaleTransactionRequestDTO = {
      idShowtime:     this.resumen.idShowtime,
      idVenue:        this.resumen.idVenue,
      asientosIds:    this.resumen.asientosIds,

      tickets: this.resumen.tickets.map((t: any) => ({
        categoryCode: t.categoryCode,
        cantidad:     t.cantidad
      })),

      snacks: (this.resumen.snacks ?? []).map((s: any) => ({
        idSnack:  s.idSnack,
        cantidad: s.cantidad
      })),

      // Total que vio el cliente: si el servidor calcula otro, responde 409.
      totalAmount:   this.totalFinal,
      paymentMethod: this.metodoPago.toUpperCase() as 'TARJETA' | 'YAPE',
      promotionCode: this.cuponAplicado
    };

    this.saleTransactionService.createSaleTransaction(request).subscribe({
      next: (res: SaleTransactionResponseDTO) => {
        this.transaccionRespuesta = res;
        this.procesando           = false;
        this.pagoExitoso          = true;
        this.bookingService.limpiar();
        this.generarQr(res.qrCodeData);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.procesando = false;
        const msg = typeof err.error === 'string'
          ? err.error
          : (err.error?.message ?? 'Ocurrió un error al procesar el pago. Intenta de nuevo.');
        this.errorPago = msg;
        if (err.status === 409) {
          this.cotizar(this.cuponAplicado);  // muestra el total nuevo
        }
        this.cdr.detectChanges();
      }
    });
  }

  // ── Helpers de la pantalla de éxito ──────────────────────────────────────
  // El QR se dibuja en el navegador: el código de la compra no sale a servicios de terceros.
  qrDataUrl = '';

  private generarQr(codigo: string): void {
    toDataURL(codigo, { width: 512, margin: 1 })
      .then(url => { this.qrDataUrl = url; this.cdr.detectChanges(); })
      .catch(() => { this.qrDataUrl = ''; this.cdr.detectChanges(); });
  }

  descargarTicket(): void {
    if (!this.qrDataUrl) return;
    const enlace = document.createElement('a');
    enlace.href = this.qrDataUrl;
    enlace.download = `cinemax-${this.transaccionRespuesta?.qrCodeData ?? 'ticket'}.png`;
    enlace.click();
  }

  // "2026-11-13 19:30:00" → "Viernes 13 de noviembre" y "19:30"
  get fechaTicket(): string {
    const fecha = this.resumenSnapshot?.fechaHora?.split(' ')[0];
    if (!fecha) return '';
    const dia = new Date(`${fecha}T00:00:00`);
    if (isNaN(dia.getTime())) return fecha;
    const texto = dia.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' }).replace(',', '');
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  }

  get horaTicket(): string {
    return this.resumenSnapshot?.fechaHora?.split(' ')[1]?.slice(0, 5) ?? '';
  }

  regresar(): void {
    this.router.navigate([this.resumen.idShowtime ? '/snacks' : '/confiteria']);
  }

  volverAlInicio(): void {
    this.router.navigate(['/movies']);
  }
}