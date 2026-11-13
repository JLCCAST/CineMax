// client-layout.ts
import { Component, OnInit, HostListener, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Offcanvas } from 'bootstrap';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { finalize, delay } from 'rxjs/operators';

@Component({
  selector: 'app-client-layout',
  standalone: true,
  // ReactiveFormsModule en lugar de FormsModule
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './client-layout.html',
  styleUrl: './client-layout.css',
})
export class ClientLayout implements OnInit {

  // FormGroups reactivos (reemplazan a loginData, registerData y correoRecuperacion)
  loginForm!: FormGroup;
  registerForm!: FormGroup;
  recuperarForm!: FormGroup;

  isLogged: boolean = false;
  userEmail: string = '';
  menuAbierto: boolean = false;
  vistaActiva: string = 'login';

  userFullName: string = 'Usuario';
  userInitials: string = 'U';
  userImageUrl: string | null = null;

  mostrarPassword = false;
  mostrarPasswordReg = false;
  cargandoLogin: boolean = false;

  mensajeAuth: string = '';
  esErrorAuth: boolean = false;
  loadingRecuperacion: boolean = false;
  mensajeRecuperacion: { texto: string, tipo: 'success' | 'danger' } | null = null;

  // Regex igual al @Pattern del backend (RegisterRequestDTO)
  private readonly PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/;

  constructor(
    private fb: FormBuilder,          
    private authService: AuthService,
    private userService: UserService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    // Inicialización de formularios reactivos
    this.loginForm = this.fb.group({
      email:    ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    this.registerForm = this.fb.group({
      firstName:      ['', [Validators.required, Validators.minLength(2)]],
      lastName:       ['', [Validators.required, Validators.minLength(2)]],
      idDocumentType: [1,  [Validators.required]],
      documentNumber: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(12)]],
      email:          ['', [Validators.required, Validators.email]],
      password:       ['', [Validators.required, Validators.pattern(this.PASSWORD_PATTERN)]]
    });

    this.recuperarForm = this.fb.group({
      correoRecuperacion: ['', [Validators.required, Validators.email]]
    });

    this.verificarSesion();

    // ?sesion=requerida (lo pone authGuard) o ?sesion=vencida (lo pone el admin).
    this.route.queryParamMap.subscribe(params => {
      const motivo = params.get('sesion');
      if (!motivo) return;
      this.pedirInicioDeSesion(motivo === 'vencida'
        ? 'Tu sesión venció. Vuelve a iniciar sesión.'
        : 'Inicia sesión para continuar.');
      this.router.navigate([], { queryParams: { sesion: null }, queryParamsHandling: 'merge', replaceUrl: true });
    });

    this.userService.user$.subscribe(user => {
      if (user) {
        this.userFullName = `${user.firstName} ${user.lastName}`.trim();
        this.userInitials = this.obtenerIniciales(user.firstName, user.lastName);
        this.userImageUrl = user.imageUrl || null;
        this.cdr.detectChanges();
      }
    });
  }

  // Helper para acceso rápido a los controles (para mostrar errores en el HTML)
  get lf() { return this.loginForm.controls; }
  get rf() { return this.registerForm.controls; }
  get rec() { return this.recuperarForm.controls; }

  cargarDatosUsuario() {
    this.userService.getProfile().subscribe({
      error: (err) => console.error('Error al cargar perfil rápido:', err)
    });
  }

  obtenerIniciales(nombre: string, apellido: string): string {
    const n = nombre ? nombre.charAt(0).toUpperCase() : '';
    const a = apellido ? apellido.charAt(0).toUpperCase() : '';
    return n + a || 'U';
  }

  cambiarVista(vista: string, event?: Event) {
    if (event) { event.preventDefault(); event.stopPropagation(); }
    this.vistaActiva = vista;
    this.mensajeAuth = '';
    this.cdr.detectChanges();
  }

  toggleMostrarPassword() { this.mostrarPassword = !this.mostrarPassword; this.cdr.detectChanges(); }
  toggleMostrarPasswordReg() { this.mostrarPasswordReg = !this.mostrarPasswordReg; this.cdr.detectChanges(); }

  iniciarSesion() {
    this.mensajeAuth = '';
    // Marcar todos los campos como tocados para mostrar errores
    this.loginForm.markAllAsTouched();

    if (this.loginForm.invalid) {
      this.esErrorAuth = true;
      this.mensajeAuth = 'Por favor ingresa un correo válido y tu contraseña.';
      this.cdr.detectChanges();
      return;
    }

    this.cargandoLogin = true;
    this.cdr.detectChanges();

    // LoginForm.value en lugar de loginData
    this.authService.login(this.loginForm.value).subscribe({
      next: (res: any) => {
        this.esErrorAuth = false;
        this.mensajeAuth = '¡Sesión iniciada con éxito! Entrando...';
        this.cargandoLogin = false;
        this.cdr.detectChanges();

        setTimeout(() => {
          document.getElementById('btn-cerrar-panel')?.click();
          this.verificarSesion();
          this.loginForm.reset();   // ✅ CAMBIO
          this.mensajeAuth = '';
          this.cdr.detectChanges();

          const rolUsuario = this.authService.getRole();
          if (rolUsuario === 'ADMIN' || (rolUsuario && rolUsuario.toUpperCase().startsWith('GERENTE'))) {
            this.router.navigate([this.authService.getDefaultRoute()]);
          }
        }, 1500);
      },
      error: (err: any) => {
        this.cargandoLogin = false;
        this.esErrorAuth = true;
        this.mensajeAuth = 'Correo o contraseña incorrectos.';
        this.cdr.detectChanges();
      }
    });
  }

  registrarse() {
    this.mensajeAuth = '';
    this.registerForm.markAllAsTouched();

    if (this.registerForm.invalid) {
      this.esErrorAuth = true;
      this.mensajeAuth = 'Por favor completa todos los campos correctamente.';
      this.cdr.detectChanges();
      return;
    }

    // RegisterForm.value en lugar de registerData
    this.authService.register(this.registerForm.value).subscribe({
      next: () => {
        this.esErrorAuth = false;
        this.mensajeAuth = '¡Registro exitoso! Redirigiendo al login...';
        this.cdr.detectChanges();
        setTimeout(() => { this.cambiarVista('login'); this.registerForm.reset({ idDocumentType: 1 }); }, 2000);
      },
      error: (err: any) => {
        this.esErrorAuth = true;
        this.mensajeAuth = err.error?.message || 'Error al registrarse. Intenta de nuevo.';
        this.cdr.detectChanges();
      }
    });
  }

  solicitarRecuperacion() {
    this.mensajeRecuperacion = null;
    this.recuperarForm.markAllAsTouched();

    if (this.recuperarForm.invalid) {
      this.mensajeRecuperacion = { texto: 'Por favor ingresa un correo electrónico válido.', tipo: 'danger' };
      this.cdr.detectChanges();
      return;
    }

    this.loadingRecuperacion = true;
    this.cdr.detectChanges();

    // RecuperarForm.value.correoRecuperacion
    this.authService.forgotPassword(this.recuperarForm.value.correoRecuperacion)
      .pipe(
        delay(3000),
        finalize(() => { this.loadingRecuperacion = false; this.cdr.detectChanges(); })
      )
      .subscribe({
        next: (res: any) => {
          this.mensajeRecuperacion = {
            texto: res.message || '¡Enlace enviado! Revisa tu bandeja de entrada.',
            tipo: 'success'
          };
          this.recuperarForm.reset();
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          const msg = err.error?.message || 'Hubo un error al conectar con el servidor.';
          this.mensajeRecuperacion = { texto: msg, tipo: 'danger' };
          this.cdr.detectChanges();
        }
      });
  }

  verificarSesion() {
    this.isLogged = this.authService.isLoggedIn();
    if (this.isLogged) {
      this.userEmail = this.authService.getEmail() || localStorage.getItem('email') || 'Mi Cuenta';
      this.cargarDatosUsuario();
    }
    this.cdr.detectChanges();
  }

  // El interceptor avisa cuando el token venció con la página abierta.
  @HostListener('window:cinemax-sesion-vencida')
  onSesionVencida() {
    this.pedirInicioDeSesion('Tu sesión venció. Vuelve a iniciar sesión.');
  }

  private pedirInicioDeSesion(mensaje: string) {
    this.isLogged = false;
    this.userFullName = 'Usuario';
    this.userInitials = 'U';
    this.userImageUrl = null;
    this.vistaActiva = 'login';
    this.mensajeAuth = mensaje;
    this.esErrorAuth = true;
    this.cdr.detectChanges();
    // El panel puede no existir todavía si esto ocurre al cargar la página.
    setTimeout(() => {
      const panel = document.getElementById('authOffcanvas');
      if (panel) Offcanvas.getOrCreateInstance(panel).show();
    });
  }

  cerrarSesion() {
    this.menuAbierto = false;
    this.authService.logout();
    this.isLogged = false;
    this.cdr.detectChanges();
    alert('Sesión cerrada correctamente.');
    this.recargarPagina(); // Llamamos al método interno
  }

  // Envolvemos el comando en una función propia del componente
  recargarPagina() {
    window.location.reload();
  }

  toggleMenu(event: Event) {
    event.preventDefault(); event.stopPropagation();
    this.menuAbierto = !this.menuAbierto;
    this.cdr.detectChanges();
  }

  @HostListener('document:click', ['$event'])
  clickFuera(event: any) {
    if (this.menuAbierto) { this.menuAbierto = false; this.cdr.detectChanges(); }
  }
}