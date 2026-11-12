import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { AdminLayout } from './layouts/admin-layout/admin-layout';
import { Venues } from './pages/admin/venues/venues';
import { Users } from './pages/admin/users/users';
import { Dashboard } from './pages/admin/dashboard/dashboard';
import { authGuard } from './services/auth.guard';
import { checkoutGuard } from './services/checkout-guard';
import { MovieDetail } from './pages/movie-detail/movie-detail';
import { Movies } from './pages/movies/movies';
import { Seats } from './pages/checkout/seats/seats';
import { Tickets } from './pages/checkout/tickets/tickets';
import { ClientLayout } from './layouts/client-layout/client-layout';
import { Snacks } from './pages/checkout/snacks/snacks';
import { Payment } from './pages/checkout/payment/payment';
import { ResetPasswordComponent } from './pages/reset-password/reset-password';
import { Profile } from './pages/profile/profile';
import { Sedes } from './pages/sedes/sedes';
import { Rooms } from './pages/admin/rooms/rooms';
import { AdminMovies } from './pages/admin/movies/movies';
import { AdminShowtimes } from './pages/admin/showtimes/showtimes';
import { Confiteria } from './pages/admin/confiteria/confiteria';
import { AdminPromotions } from './pages/admin/promotions/promotions';
import { Confiteria as ConfiteriaPublica } from './pages/confiteria/confiteria';

export const routes: Routes = [
  // ── Rutas públicas (cliente y no autenticado)
  {
    path: '',
    component: ClientLayout,
    children: [
      { path: '', component: Home },
      { path: 'profile', component: Profile },
      { path: 'movies', component: Movies },
      { path: 'sedes', component: Sedes },
      { path: 'confiteria', component: ConfiteriaPublica },
      { path: 'movie/:id', component: MovieDetail, runGuardsAndResolvers: 'always' },
      { path: 'seats', component: Seats },
      { path: 'tickets', component: Tickets, canActivate: [checkoutGuard] },
      { path: 'snacks', component: Snacks, canActivate: [checkoutGuard] },
      { path: 'payment', component: Payment, canActivate: [checkoutGuard] },
      { path: 'reset-password', component: ResetPasswordComponent }
    ]
  },

  // Rutas administrativas
  {
    path: 'admin',
    component: AdminLayout,
    children: [
      {
        path: 'dashboard',
        component: Dashboard,
        canActivate: [authGuard],
        data: { expectedPermissions: ['VIEW_DASHBOARD'] }
      },

      // Sedes: ADMIN (VIEW_VENUES) y GERENTE_GENERAL (VIEW_VENUES + MANAGE_VENUES)
      {
        path: 'sedes',
        component: Venues,
        canActivate: [authGuard],
        data: { expectedPermissions: ['VIEW_VENUES'] }
      },

      // Salas: GERENTE_GENERAL y GERENTE_DE_OPERACIONES
      {
        path: 'salas',
        component: Rooms,
        canActivate: [authGuard],
        data: { expectedPermissions: ['MANAGE_ROOMS'] }
      },

      // Películas: GERENTE_GENERAL y GERENTE_DE_OPERACIONES
      {
        path: 'peliculas',
        component: AdminMovies,
        canActivate: [authGuard],
        data: { expectedPermissions: ['MANAGE_MOVIES'] }
      },

      // Funciones: GERENTE_GENERAL y GERENTE_DE_OPERACIONES
      {
        path: 'funciones',
        component: AdminShowtimes,
        canActivate: [authGuard],
        data: { expectedPermissions: ['MANAGE_SHOWTIMES'] }
      },

      // Confitería: GERENTE_GENERAL y GERENTE_DE_MARKETING
      {
        path: 'confiteria',
        component: Confiteria,
        canActivate: [authGuard],
        data: { expectedPermissions: ['MANAGE_CONFITERIA'] }
      },

      // Promociones: GERENTE_GENERAL y GERENTE_DE_MARKETING
      {
        path: 'promociones',
        component: AdminPromotions,
        canActivate: [authGuard],
        data: { expectedPermissions: ['MANAGE_PROMOTIONS'] }
      },

      // Usuarios: solo ADMIN
      {
        path: 'usuarios',
        component: Users,
        canActivate: [authGuard],
        data: { expectedPermissions: ['MANAGE_USERS'] }
      },

      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  { path: 'perfil', component: Profile },
  { path: '**', redirectTo: '' }
];
