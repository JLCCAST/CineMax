import { Component, OnInit, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-navbar-admin',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './navbar-admin.html',
  styleUrl: './navbar-admin.css'
})
export class NavbarAdmin implements OnInit {

  rolActual: string = '';
  nombreUsuario: string = '';
  dropdownAbierto: boolean = false;
  abrirMenu = output<void>();

  get inicial(): string {
    return this.nombreUsuario.charAt(0).toUpperCase() || 'U';
  }

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.rolActual = this.authService.getRole() || '';
    this.nombreUsuario = this.authService.getFirstName() || 'Usuario';
  }

  formatearRol(roleName: string): string {
    if (!roleName) return '';
    return roleName
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, letra => letra.toUpperCase());
  }

  toggleDropdown(): void {
    this.dropdownAbierto = !this.dropdownAbierto;
  }

  cerrarDropdown(): void {
    this.dropdownAbierto = false;
  }

  cerrarSesion(): void {
    this.authService.logout();
    this.cerrarDropdown();
    this.router.navigate(['/']);
  }
}