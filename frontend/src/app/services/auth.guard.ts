import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Verificar que el usuario esté logueado
  // Sin sesión (o vencida): a la portada, que abre el panel de inicio de sesión.
  const habiaSesion = !!localStorage.getItem('cinemax_token');
  if (!authService.isLoggedIn()) {
    router.navigate(['/'], { queryParams: { sesion: habiaSesion ? 'vencida' : 'requerida' } });
    return false;
  }

  // Verificar permisos requeridos por la ruta
  const permisosRequeridos = route.data['expectedPermissions'] as string[] | undefined;

  if (permisosRequeridos && permisosRequeridos.length > 0) {
    const tienePermiso = permisosRequeridos.some(p => authService.hasPermission(p));

    if (!tienePermiso) {
      alert('Acceso Denegado: No tienes permisos para ver esta pantalla.');
      router.navigate([authService.getDefaultRoute()]);
      return false;
    }
  }

  return true;
};