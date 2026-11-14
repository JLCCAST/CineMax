import { Component, input, output } from '@angular/core';
import { DecimalPipe } from '@angular/common';

export interface SnackVenta {
  nameSnack: string;
  descriptionSnack: string;
  price: number;
  imageUrlSnack: string;
  stock: number;
}

// Producto de confitería con su contador, para la confitería pública y la del checkout.
@Component({
  selector: 'app-tarjeta-snack',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './tarjeta-snack.html',
  styleUrl: './tarjeta-snack.css'
})
export class TarjetaSnack {
  snack = input.required<SnackVenta>();
  cantidad = input(0);
  sumar = output<void>();
  restar = output<void>();

  readonly snackGenerico = 'Imagenes/snack-generico.svg';
}
