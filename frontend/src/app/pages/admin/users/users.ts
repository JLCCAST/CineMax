import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { UserService } from '../../../services/user.service';
import { VenueService } from '../../../services/venue.service';
import { RoleService, Role } from '../../../services/role.service';

const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './users.html',
  styleUrl: './users.css',
})
export class Users implements OnInit {
  listaUsuarios: any[] = [];
  listaSedes: any[] = [];
  private todasLasSedes: { idVenue?: number; nameVenue?: string }[] = [];
  listaRoles: Role[] = [];       // roles que vienen de la BD
  cargando: boolean = false;

  usuarioSeleccionado: any = null;

  //  Formulario: Cambiar Rol
  cambiarRolForm!: FormGroup;
  formRolEnviado = false;
  guardandoRol = false;
  errorServidorRol: string | null = null;

  // Formulario: Nuevo Colaborador 
  nuevoUsuarioForm!: FormGroup;
  formEnviado = false;          
  creandoUsuario = false;       
  errorServidor: string | null = null;

  mostrarSedeCreate = false;
  mostrarSedeUpdate = false;

  filtroEstado: string = 'Activo';
  filtroRol: string = 'TODOS';
  filtroSede: string = 'TODOS';

  private rolSeleccionadoRequiereSede(idRole: number): boolean {
    const rol = this.listaRoles.find(r => r.idRole === idRole);
    if (!rol) return false;
    return rol.roleName === 'GERENTE_MARKETING'
        || rol.roleName === 'GERENTE_OPERACIONES';
  }

  private rolSeleccionadoEsGlobal(idRole: number): boolean {
    const rol = this.listaRoles.find(r => r.idRole === idRole);
    if (!rol) return false;
    return rol.roleName === 'ADMIN' || rol.roleName === 'GERENTE_GENERAL';
  }

  rolActualRequiereSede(): boolean {
    const idRole = this.nuevoUsuarioForm?.get('idRole')?.value;
    return this.rolSeleccionadoRequiereSede(idRole);
  }

  // Getters para los filtros

  get rolesDisponibles() {
    return [...new Set(this.listaUsuarios.map(u => u.roleName))];
  }

  // El servicio de usuarios solo guarda el id de la sede; el nombre sale de la lista de sedes.
  nombreSede(user: any): string {
    if (!user.idVenue) return 'Todas';
    return this.todasLasSedes.find(s => s.idVenue === user.idVenue)?.nameVenue ?? `Sede ${user.idVenue}`;
  }

  get sedesDisponibles() {
    return [...new Set(this.listaUsuarios.map(u => this.nombreSede(u)))];
  }

  get usuariosFiltrados() {
    return this.listaUsuarios.filter(user => {
      const cumpleEstado = this.filtroEstado === 'TODOS' || user.status === this.filtroEstado;
      const cumpleRol    = this.filtroRol === 'TODOS'    || user.roleName === this.filtroRol;
      const cumpleSede   = this.filtroSede === 'TODOS'   || this.nombreSede(user) === this.filtroSede;
      return cumpleEstado && cumpleRol && cumpleSede;
    });
  }

  constructor(
    private userService: UserService,
    private venueService: VenueService,
    private roleService: RoleService,
    private cdr: ChangeDetectorRef,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    // El ADMIN no gestiona sedes: los nombres salen de la lista pública.
    this.venueService.getPublicVenues().subscribe({
      next: sedes => { this.todasLasSedes = sedes; this.cdr.detectChanges(); }
    });
    this.construirFormularioNuevoUsuario();
    this.construirFormularioCambiarRol();
    this.cargarUsuarios();
    this.cargarRoles();
  }

  private construirFormularioCambiarRol(): void {
    this.cambiarRolForm = this.fb.group({
      idRole: [0, [Validators.required, this.rolValidoValidator()]],
      idVenue: [0],
    });

    this.cambiarRolForm.get('idRole')?.valueChanges.subscribe(() => {
      this.actualizarValidacionSedeRol();
    });
  }

  private actualizarValidacionSedeRol(): void {
    const idRoleControl = this.cambiarRolForm.get('idRole');
    const idVenueControl = this.cambiarRolForm.get('idVenue');
    if (!idRoleControl || !idVenueControl) return;

    const idRole = idRoleControl.value;

    if (this.rolSeleccionadoEsGlobal(idRole)) {
      this.mostrarSedeUpdate = false;
      idVenueControl.clearValidators();
      idVenueControl.setValue(0);
    } else if (this.rolSeleccionadoRequiereSede(idRole)) {
      this.mostrarSedeUpdate = true;
      idVenueControl.setValidators([Validators.required, this.sedeValidaValidator()]);
      this.venueService.getAvailableVenuesForRole(idRole).subscribe({
        next: (sedes) => { this.listaSedes = sedes; this.cdr.detectChanges(); },
        error: (err) => console.error('Error al traer sedes:', err)
      });
    } else {
      this.mostrarSedeUpdate = false;
      idVenueControl.clearValidators();
      idVenueControl.setValue(0);
    }
    idVenueControl.updateValueAndValidity();
  }

  campoRolInvalido(nombreControl: string): boolean {
    const control = this.cambiarRolForm.get(nombreControl);
    if (!control) return false;
    return control.invalid && (control.touched || this.formRolEnviado);
  }

  // Construcción del formulario reactivo

  private construirFormularioNuevoUsuario(): void {
    this.nuevoUsuarioForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      idDocumentType: [1, [Validators.required]],
      documentNumber: ['', [Validators.required, Validators.pattern(/^[0-9A-Za-z]{6,15}$/)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
      idRole: [0, [Validators.required, this.rolValidoValidator()]],
      idVenue: [0],
    });

    // Cada vez que cambia el rol, revalidamos la sede (obligatoria solo para ciertos roles)
    this.nuevoUsuarioForm.get('idRole')?.valueChanges.subscribe(() => {
      this.actualizarValidacionSede();
    });
  }

  private rolValidoValidator() {
    return (control: AbstractControl): ValidationErrors | null => {
      return control.value === 0 || control.value === null ? { required: true } : null;
    };
  }

  // Activa/desactiva el Validators.required de idVenue según el rol elegido,
  private actualizarValidacionSede(): void {
    const idRoleControl = this.nuevoUsuarioForm.get('idRole');
    const idVenueControl = this.nuevoUsuarioForm.get('idVenue');
    if (!idRoleControl || !idVenueControl) return;

    const idRole = idRoleControl.value;

    if (this.rolSeleccionadoEsGlobal(idRole)) {
      this.mostrarSedeCreate = false;
      idVenueControl.clearValidators();
      idVenueControl.setValue(0);
    } else if (this.rolSeleccionadoRequiereSede(idRole)) {
      this.mostrarSedeCreate = true;
      idVenueControl.setValidators([Validators.required, this.sedeValidaValidator()]);
      this.venueService.getAvailableVenuesForRole(idRole).subscribe({
        next: (sedes) => { this.listaSedes = sedes; this.cdr.detectChanges(); },
        error: (err) => console.error('Error al traer sedes:', err)
      });
    } else {
      this.mostrarSedeCreate = false;
      idVenueControl.clearValidators();
      idVenueControl.setValue(0);
    }
    idVenueControl.updateValueAndValidity();
  }

  private sedeValidaValidator() {
    return (control: AbstractControl): ValidationErrors | null => {
      return control.value === 0 || control.value === null ? { required: true } : null;
    };
  }

  campoInvalido(nombreControl: string): boolean {
    const control = this.nuevoUsuarioForm.get(nombreControl);
    if (!control) return false;
    return control.invalid && (control.touched || this.formEnviado);
  }

  // Carga de datos

  cargarUsuarios() {
    this.cargando = true;
    this.userService.getUsers().subscribe({
      next: (datos) => {
        this.listaUsuarios = datos;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al cargar usuarios:', err);
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  cargarRoles() {
    this.roleService.getRoles().subscribe({
      next: (roles) => {
        this.listaRoles = roles.filter(r => r.roleName !== 'CLIENTE');
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error al cargar roles:', err)
    });
  }

  abrirModalRol(usuario: any) {
    this.usuarioSeleccionado = usuario;
    this.formRolEnviado = false;
    this.errorServidorRol = null;
    this.cambiarRolForm.reset({
      idRole: usuario.idRole,
      idVenue: usuario.idVenue || 0
    });
    this.actualizarValidacionSedeRol();
  }

  // Se llama al abrir el modal "Nuevo Colaborador" para asegurar que arranca limpio
  abrirModalNuevoUsuario() {
    this.formEnviado = false;
    this.errorServidor = null;
    this.mostrarSedeCreate = false;
    this.nuevoUsuarioForm.reset({
      firstName: '', lastName: '', idDocumentType: 1, documentNumber: '',
      email: '', password: '', idRole: 0, idVenue: 0
    });
  }

  guardarNuevoRol() {
    this.formRolEnviado = true;
    this.errorServidorRol = null;

    if (this.cambiarRolForm.invalid) {
      this.cambiarRolForm.markAllAsTouched();
      return;
    }
    if (!this.usuarioSeleccionado) return;

    const { idRole, idVenue } = this.cambiarRolForm.value;
    const payloadUpdate = {
      idRole,
      idVenue: idVenue === 0 ? null : idVenue
    };

    this.guardandoRol = true;
    this.userService.updateUserRole(this.usuarioSeleccionado.idUser, payloadUpdate).subscribe({
      next: () => {
        this.guardandoRol = false;
        alert('¡Rol y permisos actualizados con éxito!');
        document.getElementById('cerrarModalRol')?.click();
        this.cargarUsuarios();
      },
      error: (err) => {
        this.guardandoRol = false;
        console.error('Error al actualizar:', err);
        this.errorServidorRol =
          err?.error?.message
          || (typeof err?.error === 'string' ? err.error : null)
          || 'Hubo un error al actualizar los permisos.';
      }
    });
  }

  // Crear colaborador

  guardarNuevoUsuario() {
    this.formEnviado = true;
    this.errorServidor = null;

    if (this.nuevoUsuarioForm.invalid) {
      this.nuevoUsuarioForm.markAllAsTouched();
      return;
    }

    this.creandoUsuario = true;
    const payload = this.nuevoUsuarioForm.value;

    this.userService.createUser(payload).subscribe({
      next: () => {
        this.creandoUsuario = false;
        alert('¡Colaborador creado exitosamente!');
        document.getElementById('cerrarModalNuevoUser')?.click();
        this.cargarUsuarios();
        this.abrirModalNuevoUsuario(); 
      },
      error: (err) => {
        this.creandoUsuario = false;
        console.error('Error al crear usuario:', err);
        this.errorServidor =
          err?.error?.message
          || (typeof err?.error === 'string' ? err.error : null)
          || 'No se pudo crear el colaborador. Verifica que el correo y el documento no estén ya registrados.';
      }
    });
  }

  desactivarUsuario(idUser: number) {
    const confirmar = confirm('¿Estás seguro de que deseas desactivar a este colaborador?');
    if (confirmar) {
      this.userService.deleteUser(idUser).subscribe({
        next: () => {
          alert('Colaborador desactivado con éxito.');
          this.cargarUsuarios();
        },
        error: (err) => {
          console.error('Error al desactivar:', err);
          alert('Hubo un error al intentar desactivar el usuario.');
        }
      });
    }
  }

  activarUsuario(idUser: number) {
    const confirmar = confirm('¿Deseas reactivar a este colaborador para que recupere su acceso al sistema?');
    if (confirmar) {
      this.userService.activateUser(idUser).subscribe({
        next: () => {
          alert('¡Colaborador reactivado con éxito!');
          this.cargarUsuarios();
        },
        error: (err) => {
          console.error('Error al reactivar:', err);
          alert(err.error?.message || err.error || 'Hubo un error al intentar reactivar el usuario.');
        }
      });
    }
  }

  formatearRol(roleName: string): string {
    if (!roleName) return '';
    return roleName
      .replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, letra => letra.toUpperCase());
  }
}