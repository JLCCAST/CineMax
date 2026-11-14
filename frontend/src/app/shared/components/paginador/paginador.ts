import { Component, computed, input, output } from '@angular/core';

// Navegación entre páginas de un listado. Las páginas empiezan en 0, como en el backend.
@Component({
  selector: 'app-paginador',
  standalone: true,
  template: `
    @if (totalPaginas() > 1) {
      <nav class="paginador" aria-label="Páginas">
        <span class="resumen">{{ desde() }}–{{ hasta() }} de {{ total() }}</span>
        <button type="button" [disabled]="pagina() === 0" (click)="cambiar.emit(pagina() - 1)" aria-label="Página anterior">
          <i class="fa-solid fa-chevron-left"></i></button>
        @for (n of numeros(); track n) {
          <button type="button" [class.activa]="n === pagina()" [attr.aria-current]="n === pagina() ? 'page' : null"
                  (click)="cambiar.emit(n)">{{ n + 1 }}</button>
        }
        <button type="button" [disabled]="pagina() >= totalPaginas() - 1" (click)="cambiar.emit(pagina() + 1)" aria-label="Página siguiente">
          <i class="fa-solid fa-chevron-right"></i></button>
      </nav>
    }
  `,
  styles: `
    .paginador { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 0.35rem; margin-top: 1rem; }
    .resumen { margin-right: auto; color: var(--ink-muted); font-size: 0.88rem; }
    button { min-width: 34px; height: 34px; padding: 0 0.5rem; border: 1px solid var(--line); border-radius: var(--radius);
             background: #fff; color: var(--ink); font-weight: 600; }
    button.activa { border-color: var(--brand); background: var(--brand); color: #fff; }
    button:disabled { color: #c3c1cc; }
  `
})
export class Paginador {
  pagina = input.required<number>();
  totalPaginas = input.required<number>();
  total = input.required<number>();
  tamano = input.required<number>();
  cambiar = output<number>();

  desde = computed(() => this.pagina() * this.tamano() + 1);
  hasta = computed(() => Math.min((this.pagina() + 1) * this.tamano(), this.total()));

  // Hasta 5 números alrededor de la página actual.
  numeros = computed(() => {
    const total = this.totalPaginas();
    const inicio = Math.max(0, Math.min(this.pagina() - 2, total - 5));
    return Array.from({ length: Math.min(5, total) }, (_, i) => inicio + i);
  });
}
