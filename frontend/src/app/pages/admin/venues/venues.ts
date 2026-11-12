import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { VenueService } from '../../../services/venue.service';
import { LocationService } from '../../../services/location.service';
import * as L from 'leaflet';

export interface Venue {
  idVenue?: number;
  nameVenue: string;
  addressVenue: string;
  phoneNumber: string;
  status: string;
  idDepartment?: number;
  idProvince?: number;
  idDistrict?: number;
  departmentName?: string;
  provinceName?: string;
  districtName?: string;
  imageUrl?: string;
  latitude?: number;
  longitude?: number;
}

const COORDENADAS_LIMA_DEFAULT = { lat: -12.046374, lng: -77.042793 };

@Component({
  selector: 'app-venues',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './venues.html',
  styleUrl: './venues.css'
})
export class Venues implements OnInit {

  esEdicion: boolean = false;
  sedeAEditarId: number = 0;
  listaSedes: Venue[] = [];

  listaDepartamentos: any[] = [];
  listaProvincias: any[] = [];
  listaDistritos: any[] = [];

  cargando: boolean = false;
  guardandoSede: boolean = false;
  textoBusquedaMapa: string = '';

  map: L.Map | undefined;
  marker: L.Marker | undefined;

  sedeForm!: FormGroup;
  formEnviado = false;

  latitude: number = COORDENADAS_LIMA_DEFAULT.lat;
  longitude: number = COORDENADAS_LIMA_DEFAULT.lng;

  constructor(
    private venueService: VenueService,
    private locationService: LocationService,
    private cdr: ChangeDetectorRef,
    private fb: FormBuilder
  ) {}

  ngOnInit() {
    this.construirFormulario();
    this.cargarSedes();
    this.cargarDepartamentos();
  }

  private construirFormulario(): void {
    this.sedeForm = this.fb.group({
      nameVenue: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      phoneNumber: ['', [Validators.pattern(/^[0-9-\s]{6,15}$/)]],
      addressVenue: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(150)]],
      idDepartment: [0, [Validators.required, this.opcionValidaValidator()]],
      idProvince: [0, [Validators.required, this.opcionValidaValidator()]],
      idDistrict: [0, [Validators.required, this.opcionValidaValidator()]],
      status: ['Activo'],
    });
  }

  // Rechaza el valor "0" (placeholder de "Seleccione...")
  private opcionValidaValidator() {
    return (control: any) => (control.value === 0 || control.value === null) ? { required: true } : null;
  }

  campoInvalido(nombreControl: string): boolean {
    const control = this.sedeForm.get(nombreControl);
    if (!control) return false;
    return control.invalid && (control.touched || this.formEnviado);
  }

  cargarSedes() {
    this.cargando = true;
    this.venueService.getVenues().subscribe({
      next: (datos) => {
        this.listaSedes = datos;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error("Error al traer sedes:", err);
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  cargarDepartamentos() {
    this.locationService.getDepartments().subscribe(datos => {
      this.listaDepartamentos = datos;
      this.cdr.detectChanges();
    });
  }

  onDepartamentoChange() {
    this.sedeForm.patchValue({ idProvince: 0, idDistrict: 0 });
    this.listaProvincias = [];
    this.listaDistritos = [];

    const idDepartment = this.sedeForm.get('idDepartment')?.value;
    if (idDepartment > 0) {
      this.locationService.getProvinces(idDepartment).subscribe(datos => {
        this.listaProvincias = datos;
        this.cdr.detectChanges();
      });
    }
  }

  onProvinciaChange() {
    this.sedeForm.patchValue({ idDistrict: 0 });
    this.listaDistritos = [];

    const idProvince = this.sedeForm.get('idProvince')?.value;
    if (idProvince > 0) {
      this.locationService.getDistricts(idProvince).subscribe(datos => {
        this.listaDistritos = datos;
        this.cdr.detectChanges();
      });
    }
  }

  prepararCreacion() {
    this.esEdicion = false;
    this.sedeAEditarId = 0;
    this.formEnviado = false;
    this.imagenSeleccionada = null;
    this.imagenPrevia = null;

    this.sedeForm.reset({
      nameVenue: '', addressVenue: '', phoneNumber: '',
      status: 'Activo', idDepartment: 0, idProvince: 0, idDistrict: 0
    });
    this.listaProvincias = [];
    this.listaDistritos = [];
    this.latitude = COORDENADAS_LIMA_DEFAULT.lat;
    this.longitude = COORDENADAS_LIMA_DEFAULT.lng;

    setTimeout(() => this.initMap(), 400);
  }

  prepararEdicion(sede: any) {
    this.esEdicion = true;
    this.sedeAEditarId = sede.idVenue || 0;
    this.formEnviado = false;
    this.imagenSeleccionada = null;
    this.imagenPrevia = null;

    this.sedeForm.reset({
      nameVenue: sede.nameVenue,
      addressVenue: sede.addressVenue,
      phoneNumber: sede.phoneNumber,
      status: sede.status,
      idDepartment: sede.idDepartment || 0,
      idProvince: sede.idProvince || 0,
      idDistrict: sede.idDistrict || 0,
    });
    this.latitude = sede.latitude || COORDENADAS_LIMA_DEFAULT.lat;
    this.longitude = sede.longitude || COORDENADAS_LIMA_DEFAULT.lng;
    this.imagenUrlActual = sede.imageUrl || '';

    if (sede.idDepartment > 0) {
      this.locationService.getProvinces(sede.idDepartment).subscribe(provincias => {
        this.listaProvincias = provincias;

        if (sede.idProvince > 0) {
          this.locationService.getDistricts(sede.idProvince).subscribe(distritos => {
            this.listaDistritos = distritos;
            this.cdr.detectChanges();
          });
        }
      });
    }
    setTimeout(() => this.initMap(), 400);
  }

  guardarSede() {
    this.formEnviado = true;

    if (this.sedeForm.invalid) {
      this.sedeForm.markAllAsTouched();
      return;
    }

    const mensajeConfirmacion = this.esEdicion
      ? '¿Estás seguro de actualizar los datos de esta sede?'
      : '¿Estás seguro de registrar esta nueva sede en el sistema?';

    if (!confirm(mensajeConfirmacion)) {
      return;
    }

    const payload = {
      ...this.sedeForm.value,
      latitude: this.latitude,
      longitude: this.longitude,
    };

    const formData = new FormData();
    formData.append('venue', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    if (this.imagenSeleccionada) {
      formData.append('image', this.imagenSeleccionada);
    }

    this.guardandoSede = true;

    if (this.esEdicion) {
      this.venueService.updateVenue(this.sedeAEditarId, formData).subscribe({
        next: () => {
          this.guardandoSede = false;
          alert('¡Sede actualizada con éxito!');
          this.cerrarModal();
          this.cargarSedes();
        },
        error: (err) => {
          this.guardandoSede = false;
          const msg = err.error?.message || 'Error al actualizar la sede';
          alert(msg);
        }
      });
    } else {
      this.venueService.createVenue(formData).subscribe({
        next: () => {
          this.guardandoSede = false;
          alert('¡Sede creada con éxito!');
          this.cerrarModal();
          this.cargarSedes();
        },
        error: (err) => {
          this.guardandoSede = false;
          const msg = err.error?.message || 'Error al guardar la sede';
          alert(msg);
        }
      });
    }
  }

  toggleEstado(sede: any) {
    const nuevoEstado = sede.status === 'Activo' ? 'Inactivo' : 'Activo';
    const accionTexto = sede.status === 'Activo' ? 'desactivar (ocultar)' : 'reactivar';

    if (!confirm(`¿Estás completamente seguro de ${accionTexto} la sede "${sede.nameVenue}"?`)) {
      return;
    }

    // Enviamos los datos completos para que el backend no los rechace por @Valid.
    // Este es el camino real de "eliminar" una sede en la UI: es un soft delete vía status,
    // NO usa el endpoint DELETE /venues/{id} (que sí existe en el backend pero no se llama
    // desde ningún lugar del frontend — ver incidencia documentada sobre deleteVenue()).
    const requestBody = {
      nameVenue: sede.nameVenue,
      addressVenue: sede.addressVenue,
      phoneNumber: sede.phoneNumber,
      status: nuevoEstado,
      idDepartment: sede.idDepartment || 0,
      idProvince: sede.idProvince || 0,
      idDistrict: sede.idDistrict || 0,
      latitude: sede.latitude,
      longitude: sede.longitude
    };

    const formData = new FormData();
    formData.append('venue', new Blob([JSON.stringify(requestBody)], { type: 'application/json' }));

    this.venueService.updateVenue(sede.idVenue, formData).subscribe({
      next: () => {
        alert(`¡Sede ${accionTexto}a con éxito!`);
        this.cargarSedes();
      },
      error: (err) => {
        const msg = err.error?.message || 'Error al cambiar el estado de la sede';
        alert(msg);
      }
    });
  }

  cerrarModal() {
    document.getElementById('btnCerrarModalSede')?.click();

    setTimeout(() => {
      this.esEdicion = false;
      this.formEnviado = false;
      this.sedeForm.reset({
        nameVenue: '', addressVenue: '', phoneNumber: '',
        status: 'Activo', idDepartment: 0, idProvince: 0, idDistrict: 0
      });
      this.latitude = COORDENADAS_LIMA_DEFAULT.lat;
      this.longitude = COORDENADAS_LIMA_DEFAULT.lng;
      this.imagenSeleccionada = null;
      this.imagenPrevia = null;
    }, 400);
  }

  // Variables para controlar la imagen
  imagenSeleccionada: File | null = null;
  imagenPrevia: string | null = null;
  imagenUrlActual: string = ''; // reemplaza a nuevaSede.imageUrl para mostrar la vista previa al editar

  // Eventos del Drag & Drop
  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.procesarArchivo(files[0]);
    }
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.procesarArchivo(file);
    }
  }

  procesarArchivo(file: File) {
    if (file.type.match(/image\/*/) == null) {
      alert("Solo se permiten imágenes (JPG, PNG, etc).");
      return;
    }

    this.imagenSeleccionada = file;

    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = (_event) => {
      this.imagenPrevia = reader.result as string;
      this.cdr.detectChanges();
    };
  }

  quitarImagen() {
    this.imagenSeleccionada = null;
    this.imagenPrevia = null;
    this.imagenUrlActual = '';
  }

  initMap() {
    if (this.map) {
      this.map.remove();
    }

    this.map = L.map('mapa-sede').setView([this.latitude, this.longitude], 15);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap'
    }).addTo(this.map);

    const customIcon = L.icon({
      iconUrl: 'Imagenes/marcador-sede.svg',
      iconSize: [35, 35],
      iconAnchor: [17, 35]
    });

    this.marker = L.marker([this.latitude, this.longitude], { icon: customIcon }).addTo(this.map);

    this.map.on('click', (e: any) => {
      const lat = e.latlng.lat;
      const lng = e.latlng.lng;

      this.latitude = lat;
      this.longitude = lng;

      if (this.marker) {
        this.marker.setLatLng([lat, lng]);
      }
      this.cdr.detectChanges(); // refleja el nuevo Lat/Lng mostrado en el HTML
    });

    this.map.invalidateSize();
  }

  buscarUbicacionLibre() {
    if (!this.textoBusquedaMapa || !this.textoBusquedaMapa.trim()) {
      alert('Escribe un lugar de referencia para buscar en el mapa.');
      return;
    }

    const query = `${this.textoBusquedaMapa}, Perú`;
    const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}`;

    fetch(url)
      .then(res => res.json())
      .then(data => {
        if (data && data.length > 0) {
          const lat = parseFloat(data[0].lat);
          const lon = parseFloat(data[0].lon);

          this.latitude = lat;
          this.longitude = lon;

          if (this.map && this.marker) {
            this.map.setView([lat, lon], 15);
            this.marker.setLatLng([lat, lon]);
          }
          this.cdr.detectChanges();
        } else {
          alert('No se encontró el lugar exacto. Intenta buscar solo por el nombre del distrito o ciudad (Ej: "Cayma, Arequipa") y luego mueve el mapa con el ratón.');
        }
      })
      .catch(err => {
        console.error('Error en búsqueda de mapa:', err);
        alert('Hubo un error de conexión con el mapa.');
      });
  }

}