import { Component, OnInit, output } from '@angular/core';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

export interface Seccion {
  ruta: string;
  texto: string;
  icono: string;
  permiso: string;
  detalle: string;
}

export const SECCIONES: Seccion[] = [
  { ruta: '/admin/dashboard',   texto: 'Inicio',      icono: 'fa-solid fa-house',        permiso: 'VIEW_DASHBOARD', detalle: 'Accesos a cada sección' },
  { ruta: '/admin/sedes',       texto: 'Sedes',       icono: 'fa-solid fa-building',     permiso: 'VIEW_VENUES', detalle: 'Cines, direcciones y ubicación en el mapa' },
  { ruta: '/admin/salas',       texto: 'Salas',       icono: 'fa-solid fa-couch',        permiso: 'MANAGE_ROOMS', detalle: 'Salas y distribución de butacas' },
  { ruta: '/admin/peliculas',   texto: 'Películas',   icono: 'fa-solid fa-film',         permiso: 'MANAGE_MOVIES', detalle: 'Cartelera, estrenos y preventa' },
  { ruta: '/admin/funciones',   texto: 'Funciones',   icono: 'fa-solid fa-clapperboard', permiso: 'MANAGE_SHOWTIMES', detalle: 'Horarios, salas y precios' },
  { ruta: '/admin/confiteria',  texto: 'Confitería',  icono: 'fa-solid fa-cookie-bite',  permiso: 'MANAGE_CONFITERIA', detalle: 'Productos, precios y stock por sede' },
  { ruta: '/admin/promociones', texto: 'Promociones', icono: 'fa-solid fa-tags',         permiso: 'MANAGE_PROMOTIONS', detalle: 'Descuentos, cupones y condiciones' },
  { ruta: '/admin/usuarios',    texto: 'Usuarios',    icono: 'fa-solid fa-users',        permiso: 'MANAGE_USERS', detalle: 'Colaboradores, roles y sedes' },
];

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css'
})
export class Sidebar implements OnInit {

  // Avisa al layout para cerrar el menú en pantallas angostas.
  navego = output<void>();

  secciones: Seccion[] = [];

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    const permisos = this.authService.getPermissions();
    this.secciones = SECCIONES.filter(s => permisos.includes(s.permiso));
  }

  cerrarSesion() {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
