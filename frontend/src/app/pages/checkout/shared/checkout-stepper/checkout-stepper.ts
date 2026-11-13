import { Component, Input } from '@angular/core';

export type CheckoutStep = 'seats' | 'tickets' | 'snacks' | 'payment';

const PASOS: { id: CheckoutStep; label: string }[] = [
  { id: 'seats',   label: 'Butacas' },
  { id: 'tickets', label: 'Entradas' },
  { id: 'snacks',  label: 'Confitería' },
  { id: 'payment', label: 'Pago' },
];

@Component({
  selector: 'app-checkout-stepper',
  standalone: true,
  templateUrl: './checkout-stepper.html',
  styleUrl: './checkout-stepper.css'
})
export class CheckoutStepperComponent {

  @Input() pasoActual: CheckoutStep = 'seats';
  // Pedido solo de confitería: no hay butacas ni entradas.
  @Input() soloConfiteria = false;

  get pasos() {
    return this.soloConfiteria ? PASOS.filter(p => p.id === 'snacks' || p.id === 'payment') : PASOS;
  }

  estado(id: CheckoutStep): 'hecho' | 'activo' | 'pendiente' {
    const orden = PASOS.findIndex(p => p.id === id);
    const actual = PASOS.findIndex(p => p.id === this.pasoActual);
    return orden < actual ? 'hecho' : orden === actual ? 'activo' : 'pendiente';
  }
}
