import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { SECCIONES } from '../../../shared/components/sidebar/sidebar';

// Inicio del panel: accesos a las secciones del rol. El tablero con indicadores
// llega con la épica 11.
@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  readonly nombre: string;
  readonly secciones;

  constructor(authService: AuthService) {
    this.nombre = authService.getFirstName() || '';
    const permisos = authService.getPermissions();
    this.secciones = SECCIONES.filter(s => s.ruta !== '/admin/dashboard' && permisos.includes(s.permiso));
  }
}
