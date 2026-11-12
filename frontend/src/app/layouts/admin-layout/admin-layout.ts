import { Component, HostListener } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Sidebar } from '../../shared/components/sidebar/sidebar';
import { NavbarAdmin } from '../../shared/components/navbar-admin/navbar-admin';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterModule, Sidebar, NavbarAdmin],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.css'
})
export class AdminLayout {

  constructor(private router: Router) {}

  // Si el token vence con el admin abierto, se vuelve a la portada para iniciar sesión otra vez.
  @HostListener('window:cinemax-sesion-vencida')
  onSesionVencida() {
    this.router.navigate(['/'], { queryParams: { sesion: 'vencida' } });
  }
}
