import { HttpInterceptorFn } from '@angular/common/http';
import { SESION_VENCIDA, tokenVencido } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  let token = localStorage.getItem('cinemax_token');
  // Si venció con la página abierta, se descarta y los layouts piden iniciar sesión otra vez.
  if (token && tokenVencido(token)) {
    localStorage.removeItem('cinemax_token');
    token = null;
    window.dispatchEvent(new CustomEvent(SESION_VENCIDA));
  }
  const hasToken = token && token.trim() !== '' && token !== 'null' && token !== 'undefined';

  const esRutaPublica = 
  req.url.includes('/api/v1/auth/login') ||
  req.url.includes('/api/v1/auth/register') ||
  req.url.includes('/api/v1/auth/forgot') ||
  (req.method === 'GET' && req.url.includes('/api/v1/snacks')) ||
  (req.method === 'GET' && req.url.includes('/api/v1/movies')) ||
  (req.method === 'GET' && req.url.includes('/api/v1/showtimes') 
    && !req.url.includes('/by-venue'));

  if (hasToken && !esRutaPublica) {
    if (req.body instanceof FormData) {
      return next(req.clone({ 
        setHeaders: { Authorization: `Bearer ${token}` } 
      }));
    }
    
    return next(req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }));
  }

  const headers: { [key: string]: string } = {};
  if (!(req.body instanceof FormData) && req.method !== 'GET') {
    headers['Content-Type'] = 'application/json';
  }

  return next(req.clone({ setHeaders: headers }));
};