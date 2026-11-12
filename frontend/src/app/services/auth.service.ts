import { jwtDecode } from 'jwt-decode';
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../environments/environment';

// Evento que avisa a los layouts que la sesión venció con la página abierta.
export const SESION_VENCIDA = 'cinemax-sesion-vencida';

export function tokenVencido(token: string): boolean {
  try {
    const { exp } = jwtDecode<{ exp?: number }>(token);
    return exp !== undefined && exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  login(credentials: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
      tap((response: any) => {
        if (response.token) {
          localStorage.setItem('cinemax_token', response.token);
        }
      })
    );
  }

  // El token dura una hora. Vencido, la API lo trata como anónimo (403), así
  // que se descarta y la app vuelve a pedir el inicio de sesión.
  getToken(): string | null {
    const token = localStorage.getItem('cinemax_token');
    if (token && tokenVencido(token)) {
      localStorage.removeItem('cinemax_token');
      return null;
    }
    return token;
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  logout() {
    localStorage.removeItem('cinemax_token');
  }

  getRole(): string {
    const token = this.getToken();
    if (token) {
      const decoded: any = jwtDecode(token);
      let role = decoded.role || 'USUARIO';
      if (role.startsWith('ROLE_')) {
        role = role.replace('ROLE_', '');
      }
      return role;
    }
    return 'INVITADO';
  }

  // Devuelve la lista de permisos del usuario logueado, leídos directamente del claim "permissions" del JWT.
  getPermissions(): string[] {
    const token = this.getToken();
    if (token) {
      try {
        const decoded: any = jwtDecode(token);
        return decoded.permissions || [];
      } catch (e) {
        return [];
      }
    }
    return [];
  }

  hasPermission(permission: string): boolean {
    return this.getPermissions().includes(permission);
  }

  // Devuelve la primera ruta admin a la que el usuario logueado realmente
  // tiene acceso, según sus permisos reales. Evita el bucle de redirigir
  // siempre a /admin/dashboard cuando el usuario (ej. ADMIN) no tiene
  // VIEW_DASHBOARD. Si no tiene ningún permiso admin, manda al home público.
  getDefaultRoute(): string {
    const rutasPorPermiso: [string, string][] = [
      ['VIEW_DASHBOARD', '/admin/dashboard'],
      ['MANAGE_USERS', '/admin/usuarios'],
      ['VIEW_VENUES', '/admin/sedes'],
      ['MANAGE_VENUES', '/admin/sedes'],
      ['MANAGE_ROOMS', '/admin/salas'],
      ['MANAGE_MOVIES', '/admin/peliculas'],
      ['MANAGE_SHOWTIMES', '/admin/funciones'],
      ['MANAGE_CONFITERIA', '/admin/confiteria'],
    ];

    const permisos = this.getPermissions();
    const match = rutasPorPermiso.find(([permiso]) => permisos.includes(permiso));
    return match ? match[1] : '/';
  }

  register(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, data).pipe(
      tap((response: any) => {
        if (response.token) {
          localStorage.setItem('cinemax_token', response.token);
        }
      })
    );
  }

  forgotPassword(email: string) {
    return this.http.post(`${this.apiUrl}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string) {
    return this.http.post(`${this.apiUrl}/reset-password`, { token, newPassword });
  }

  getEmail(): string | null {
    const token = this.getToken();
    if (token) {
      try {
        const decoded: any = jwtDecode(token);
        return decoded.sub || decoded.email || null;
      } catch (e) { return null; }
    }
    return null;
  }

  getIdVenue(): number | null {
    const token = this.getToken();
    if (token) {
      try {
        const decoded: any = jwtDecode(token);
        return decoded.idVenue ?? null;
      } catch (e) { return null; }
    }
    return null;
  }

  getFirstName(): string | null {
    const token = this.getToken();
    if (token) {
      try {
        const decoded: any = jwtDecode(token);
        return decoded.firstName ?? null;
      } catch (e) { return null; }
    }
    return null;
  }
}