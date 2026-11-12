import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { RouterModule, Router } from '@angular/router';
import { VenueService } from '../../services/venue.service';
import * as L from 'leaflet';

@Component({
  selector: 'app-sedes',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './sedes.html',
  styleUrl: './sedes.css'
})
export class Sedes implements OnInit {

  // --- Estado de datos ---
  sedes: any[] = [];
  sedesMostradas: any[] = [];
  departamentos: string[] = [];

  // --- Estado de filtros ---
  departamentoSeleccionado: string = '';
  textoBusqueda: string = '';

  // --- Estado del modal de mapa ---
  sedeDetalle: any = null;
  map: L.Map | undefined;
  marker: L.Marker | undefined;

  constructor(
    private venueService: VenueService, 
    private router: Router, 
    private cdr: ChangeDetectorRef
  ) {}

  // --- Inicialización ---

  ngOnInit() {
    this.venueService.getPublicVenues().subscribe(data => {
      this.sedes = data;

      // Extrae departamentos únicos para los tabs de filtro
      this.departamentos = [...new Set(this.sedes.map(s => s.departmentName))].filter(d => d);

      // Selecciona el primer departamento por defecto
      if (this.departamentos.length > 0) {
        this.departamentoSeleccionado = this.departamentos[0];
        this.aplicarFiltros();
      }

      this.cdr.detectChanges();
    });
  }

  // --- Filtros ---

  seleccionarDepartamento(dep: string) {
    this.departamentoSeleccionado = dep;
    this.textoBusqueda = ''; // limpia el buscador al cambiar de departamento
    this.aplicarFiltros();
  }

  // Filtra sedes por departamento activo y texto del buscador (nombre o distrito)
  aplicarFiltros() {
    this.sedesMostradas = this.sedes.filter(s => {
      const coincideDep = s.departmentName === this.departamentoSeleccionado;
      const coincideTexto =
        s.nameVenue.toLowerCase().includes(this.textoBusqueda.toLowerCase()) ||
        s.districtName?.toLowerCase().includes(this.textoBusqueda.toLowerCase());
      return coincideDep && coincideTexto;
    });
  }

  // --- Navegación ---

  // Lleva al usuario a la cartelera filtrada por sede
  irACartelera(sedeId: number) {
    this.router.navigate(['/movies'], { queryParams: { sede: sedeId } });
  }

  // --- Modal de detalles con mapa ---

  verDetalles(sede: any) {
    this.sedeDetalle = sede;
    setTimeout(() => this.initMap(sede), 300); // espera a que el DOM renderice el contenedor del mapa
  }

  cerrarDetalles() {
    this.sedeDetalle = null;
  }

  // Inicializa el mapa Leaflet centrado en la sede; usa Lima como fallback si no hay coordenadas
  initMap(sede: any) {
    if (this.map) this.map.remove();

    const lat = sede.latitude || -12.046374;
    const lng = sede.longitude || -77.042793;

    this.map = L.map('mapa-cliente').setView([lat, lng], 16);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap'
    }).addTo(this.map);

    const customIcon = L.icon({
      iconUrl: 'Imagenes/marcador-sede.svg',
      iconSize: [40, 40],
      iconAnchor: [20, 40]
    });

    this.marker = L.marker([lat, lng], { icon: customIcon }).addTo(this.map);
    this.map.invalidateSize();
  }
}