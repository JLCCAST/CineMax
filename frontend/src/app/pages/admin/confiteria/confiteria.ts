import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { ConfiteriaService } from '../../../services/confiteria.service';
import { AuthService } from '../../../services/auth.service';
import { environment } from '../../../environments/environment';
import { Modal } from 'bootstrap';

@Component({
  selector: 'app-confiteria',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './confiteria.html',
  styleUrls: ['./confiteria.css']
})
export class Confiteria implements OnInit {

  // --- Estado de datos ---
  snacks: any[] = [];
  categories: any[] = [];
  sedes: any[] = [];
  sedeSeleccionada: any = null;
  snackRecienCreado: any = null;
  snackAEliminar: number | null = null;

  // --- Estado del formulario de snack ---
  currentSnack: any = {
    nameSnack: '', descriptionSnack: '', price: null,
    stock: null, status: 'Activo', idSnackCategory: null
  };
  currentSnackId: number | null = null;
  isEditMode: boolean = false;
  selectedFile: File | null = null;

  // --- Estado de UI ---
  cargando: boolean = false;
  cargandoSedes: boolean = false;
  mostrarModalSede: boolean = false;
  mostrarModalEliminar: boolean = false;

  // --- Permisos del usuario autenticado ---
  esGerenteGeneral: boolean = false;
  idVenueAsignada: number | null = null;

  constructor(
    private confiteriaService: ConfiteriaService,
    private cdr: ChangeDetectorRef,
    private http: HttpClient,
    private authService: AuthService,
    private router: Router
  ) {}

  // --- Inicialización ---

  ngOnInit(): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/']);
      return;
    }

    // Determina el rol y la sede asignada para controlar qué ve cada usuario
    const rol = this.authService.getRole();
    this.esGerenteGeneral = rol === 'GERENTE_GENERAL';
    this.idVenueAsignada = this.authService.getIdVenue();

    this.cargandoSedes = true;
    this.cargarSedes();
    this.cargarCategorias();
  }

  // --- Carga de datos ---

  // Gerente General ve todas las sedes; otros roles solo ven la suya
  cargarSedes() {
    this.http.get<any[]>(`${environment.apiUrl}/venues/public`).subscribe({
      next: (res) => {
        this.sedes = !this.esGerenteGeneral && this.idVenueAsignada
          ? res.filter(s => s.idVenue === this.idVenueAsignada)
          : res;
        this.cargandoSedes = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoSedes = false;
        this.cdr.detectChanges();
      }
    });
  }

  cargarCategorias() {
    this.confiteriaService.cargarCategorias().subscribe({
      next: (res: any) => { this.categories = res; this.cdr.detectChanges(); },
      error: (err: any) => console.error('Error categorías:', err)
    });
  }

  // Carga todos los snacks sin filtro de sede (fallback)
  cargarSnacks() {
    this.cargando = true;
    this.confiteriaService.cargarSnacks().subscribe({
      next: (res: any) => { this.snacks = res; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.cargando = false; this.cdr.detectChanges(); }
    });
  }

  // Carga los snacks de una sede específica con autenticación (endpoint admin)
  cargarSnacksPorSede(idVenue: number) {
    this.cargando = true;
    this.cdr.detectChanges();
    const headers = new HttpHeaders({ Authorization: `Bearer ${this.authService.getToken()}` });
    this.http.get<any[]>(`${environment.apiUrl}/snacks/venue/${idVenue}/admin`, { headers }).subscribe({
      next: (res) => { this.snacks = res; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.cargando = false; this.cdr.detectChanges(); }
    });
  }

  // Recarga los snacks según si hay sede seleccionada o no
  recargarSnacks() {
    if (this.sedeSeleccionada) {
      this.cargarSnacksPorSede(this.sedeSeleccionada.idVenue);
    } else {
      this.cargarSnacks();
    }
  }

  // --- Selección de sede ---

  elegirSede(valor: string) {
    const sede = this.sedes.find((s: any) => s.idVenue === Number(valor));
    if (sede) {
      this.seleccionarSede(sede);
    } else {
      this.sedeSeleccionada = null;
      this.snacks = [];
    }
  }

  seleccionarSede(sede: any) {
    this.sedeSeleccionada = sede;
    this.cdr.detectChanges();
    this.cargarSnacksPorSede(sede.idVenue);
  }

  // --- Modal de snack (crear / editar) ---

  abrirModalNuevo() {
    this.isEditMode = false;
    this.currentSnackId = null;
    this.selectedFile = null;
    this.currentSnack = {
      nameSnack: '', descriptionSnack: '', price: null,
      stock: null, status: 'Activo', idSnackCategory: null
    };
  }

  abrirModalEditar(snack: any) {
    this.isEditMode = true;
    this.currentSnackId = snack.idSnack;
    this.selectedFile = null;
    this.currentSnack = {
      nameSnack: snack.nameSnack,
      descriptionSnack: snack.descriptionSnack,
      price: snack.price,
      stock: snack.stock,
      status: snack.status,
      idSnackCategory: snack.idSnackCategory
    };
  }

  onFileSelected(event: any) {
    if (event.target.files.length > 0) this.selectedFile = event.target.files[0];
  }

  // Cierra el modal de Bootstrap y limpia los residuos del DOM que deja el backdrop
  private cerrarModalSnack() {
    const modalEl = document.getElementById('modalSnack');
    if (modalEl) {
      const modal = Modal.getInstance(modalEl) || new Modal(modalEl);
      modal.hide();
    }
    setTimeout(() => {
      document.body.classList.remove('modal-open');
      document.body.style.removeProperty('overflow');
      document.body.style.removeProperty('padding-right');
      document.querySelectorAll('.modal-backdrop').forEach(b => b.remove());
    }, 300);
  }

  // --- Guardar snack (crear o editar) ---

  guardarSnack() {
    // Validaciones básicas antes de enviar
    if (!this.currentSnack.nameSnack || this.currentSnack.nameSnack.trim() === '') {
      alert('El nombre del producto es obligatorio');
      return;
    }
    if (!this.currentSnack.price || Number(this.currentSnack.price) <= 0) {
      alert('El precio debe ser mayor a S/. 0.00');
      return;
    }

    const headers = new HttpHeaders({ Authorization: `Bearer ${this.authService.getToken()}` });

    // Arma el FormData con los datos del snack y la imagen opcional
    const formData = new FormData();
    const snackData = JSON.stringify({
      nameSnack: this.currentSnack.nameSnack,
      descriptionSnack: this.currentSnack.descriptionSnack,
      price: Number(this.currentSnack.price),
      stock: Number(this.currentSnack.stock),
      status: this.currentSnack.status || 'Activo',
      idSnackCategory: Number(this.currentSnack.idSnackCategory)
    });
    formData.append('snack', snackData);
    if (this.selectedFile) formData.append('file', this.selectedFile);

    if (this.isEditMode && this.currentSnackId) {
      // Modo edición: actualiza el snack y luego actualiza el stock en la sede
      this.confiteriaService.actualizarSnack(this.currentSnackId, formData).subscribe({
        next: () => {
          if (this.sedeSeleccionada) {
            this.http.patch(
              `${environment.apiUrl}/snacks/${this.currentSnackId}/venue/${this.sedeSeleccionada.idVenue}/stock?stock=${this.currentSnack.stock}`,
              {},
              { headers }
            ).subscribe({
              next: () => { this.cerrarModalSnack(); this.recargarSnacks(); alert('Producto actualizado'); },
              error: (err: any) => alert('Error al actualizar stock: ' + err.message)
            });
          } else {
            this.cerrarModalSnack();
            this.recargarSnacks();
            alert('Producto actualizado');
          }
        },
        error: (err: any) => alert('Error: ' + err.message)
      });
    } else {
      // Modo creación: crea el snack y luego decide en qué sede(s) agregarlo
      this.confiteriaService.crearSnack(formData).subscribe({
        next: (res: any) => {
          this.cerrarModalSnack();
          this.snackRecienCreado = res;

          if (this.esGerenteGeneral) {
            // Gerente General elige si agregar a una sede o a todas
            this.mostrarModalSede = true;
            this.cdr.detectChanges();
          } else {
            // Otros roles agregan directo a su sede asignada
            this.agregarASede(true);
            alert('Producto agregado exitosamente a ' + this.sedeSeleccionada?.nameVenue);
          }
        },
        error: (err: any) => alert('Error: ' + err.message)
      });
    }
  }

  // --- Asignación de snack a sede(s) ---

  // solo=true → agrega solo a la sede seleccionada; solo=false → agrega a todas
  agregarASede(solo: boolean) {
    if (!this.snackRecienCreado) return;
    const headers = new HttpHeaders({ Authorization: `Bearer ${this.authService.getToken()}` });
    const stock = this.snackRecienCreado.stock ?? 0;

    const url = solo && this.sedeSeleccionada
      ? `${environment.apiUrl}/snacks/${this.snackRecienCreado.idSnack}/venue/${this.sedeSeleccionada.idVenue}/agregar?stock=${stock}`
      : `${environment.apiUrl}/snacks/${this.snackRecienCreado.idSnack}/venue/todas/agregar?stock=${stock}`;

    this.http.post(url, {}, { headers }).subscribe({
      next: () => {
        this.mostrarModalSede = false;
        this.snackRecienCreado = null;
        this.recargarSnacks();
        this.cdr.detectChanges();
      },
      error: (err: any) => alert('Error: ' + err.message)
    });
  }

  // --- Habilitar / Inhabilitar snack en una sede ---

  inhabilitarSnack(idSnack: number) {
    if (!this.sedeSeleccionada) return;
    if (confirm('¿Inhabilitar este producto en esta sede?')) {
      const headers = new HttpHeaders({ Authorization: `Bearer ${this.authService.getToken()}` });
      this.http.patch(`${environment.apiUrl}/snacks/${idSnack}/venue/${this.sedeSeleccionada.idVenue}/inhabilitar`, {}, { headers }).subscribe({
        next: () => { this.recargarSnacks(); alert('Producto inhabilitado en esta sede'); },
        error: (err: any) => alert('Error: ' + err.message)
      });
    }
  }

  habilitarSnack(idSnack: number) {
    if (!this.sedeSeleccionada) return;
    if (confirm('¿Habilitar este producto en esta sede?')) {
      const headers = new HttpHeaders({ Authorization: `Bearer ${this.authService.getToken()}` });
      this.http.patch(`${environment.apiUrl}/snacks/${idSnack}/venue/${this.sedeSeleccionada.idVenue}/habilitar`, {}, { headers }).subscribe({
        next: () => { this.recargarSnacks(); alert('Producto habilitado en esta sede'); },
        error: (err: any) => alert('Error: ' + err.message)
      });
    }
  }

  // --- Eliminación de snack ---

  // Verifica en cuántas sedes está el snack antes de decidir cómo eliminarlo
  eliminarSnack(idSnack: number) {
    if (!this.sedeSeleccionada) return;
    const headers = new HttpHeaders({ Authorization: `Bearer ${this.authService.getToken()}` });

    this.http.get<number>(`${environment.apiUrl}/snacks/${idSnack}/sedes-count`, { headers }).subscribe({
      next: (count) => {
        if (count <= 1) {
          // Solo está en una sede → elimina definitivamente sin mostrar modal
          if (confirm('¿Eliminar este producto definitivamente?')) {
            this.http.delete(`${environment.apiUrl}/snacks/${idSnack}/venue/todas`, { headers }).subscribe({
              next: () => { this.recargarSnacks(); alert('Producto eliminado'); this.cdr.detectChanges(); },
              error: (err: any) => alert('Error: ' + err.message)
            });
          }
        } else {
          // Está en varias sedes → pregunta si eliminar solo de esta sede o de todas
          this.snackAEliminar = idSnack;
          this.mostrarModalEliminar = true;
          this.cdr.detectChanges();
        }
      },
      error: (err: any) => alert('Error al verificar sedes: ' + err.message)
    });
  }

  // todas=true → elimina de todas las sedes; todas=false → solo de la sede actual
  confirmarEliminar(todas: boolean) {
    if (!this.snackAEliminar) return;
    const headers = new HttpHeaders({ Authorization: `Bearer ${this.authService.getToken()}` });

    const url = todas
      ? `${environment.apiUrl}/snacks/${this.snackAEliminar}/venue/todas`
      : `${environment.apiUrl}/snacks/${this.snackAEliminar}/venue/${this.sedeSeleccionada.idVenue}`;

    this.http.delete(url, { headers }).subscribe({
      next: () => {
        this.mostrarModalEliminar = false;
        this.snackAEliminar = null;
        this.recargarSnacks();
        alert('Producto eliminado');
        this.cdr.detectChanges();
      },
      error: (err: any) => alert('Error: ' + err.message)
    });
  }
}