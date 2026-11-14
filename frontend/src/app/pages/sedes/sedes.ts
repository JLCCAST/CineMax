import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, computed, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';
import { VenueService } from '../../services/venue.service';
import * as L from 'leaflet';

interface Sede {
  idVenue: number;
  nameVenue: string;
  addressVenue: string;
  districtName: string;
  departmentName: string;
  phoneNumber: string | null;
  latitude: number | null;
  longitude: number | null;
}

@Component({
  selector: 'app-sedes',
  standalone: true,
  templateUrl: './sedes.html',
  styleUrl: './sedes.css'
})
export class Sedes implements OnInit, AfterViewInit, OnDestroy {

  private readonly contenedorMapa = viewChild.required<ElementRef<HTMLElement>>('mapa');

  readonly sedes = signal<Sede[]>([]);
  readonly cargando = signal(true);
  readonly error = signal(false);
  readonly departamento = signal('');
  readonly busqueda = signal('');
  readonly elegida = signal<number | null>(null);

  readonly departamentos = computed(() =>
    [...new Set(this.sedes().map(s => s.departmentName).filter(Boolean))]);

  readonly mostradas = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    return this.sedes().filter(s =>
      (!this.departamento() || s.departmentName === this.departamento()) &&
      (!texto || s.nameVenue.toLowerCase().includes(texto) || s.districtName?.toLowerCase().includes(texto)));
  });

  private mapa?: L.Map;
  private marcadores = new Map<number, L.Marker>();
  private readonly icono = L.icon({ iconUrl: 'Imagenes/marcador-sede.svg', iconSize: [36, 36], iconAnchor: [18, 36], popupAnchor: [0, -32] });

  constructor(private venueService: VenueService, private router: Router) {}

  ngOnInit(): void {
    this.venueService.getPublicVenues().subscribe({
      next: (data: Sede[]) => {
        this.sedes.set(data);
        this.departamento.set(this.departamentos()[0] ?? '');
        this.cargando.set(false);
        this.pintarMarcadores();
      },
      error: () => {
        this.cargando.set(false);
        this.error.set(true);
      }
    });
  }

  ngAfterViewInit(): void {
    this.mapa = L.map(this.contenedorMapa().nativeElement, { scrollWheelZoom: false }).setView([-12.0464, -77.0428], 11);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '© OpenStreetMap' }).addTo(this.mapa);
    this.pintarMarcadores();
  }

  ngOnDestroy(): void {
    this.mapa?.remove();
  }

  elegirDepartamento(dep: string): void {
    this.departamento.set(dep);
    this.busqueda.set('');
    this.elegida.set(null);
    this.pintarMarcadores();
  }

  buscar(texto: string): void {
    this.busqueda.set(texto);
    this.pintarMarcadores();
  }

  verEnMapa(sede: Sede): void {
    this.elegida.set(sede.idVenue);
    const marcador = this.marcadores.get(sede.idVenue);
    if (!marcador || !this.mapa) return;
    this.mapa.flyTo(marcador.getLatLng(), 16, { duration: 0.6 });
    marcador.openPopup();
  }

  irACartelera(sede: Sede): void {
    localStorage.setItem('cinemax_cine', String(sede.idVenue));
    this.router.navigate(['/movies'], { queryParams: { idVenue: sede.idVenue } });
  }

  comoLlegar(sede: Sede): string {
    return `https://www.google.com/maps/dir/?api=1&destination=${sede.latitude},${sede.longitude}`;
  }

  private contenidoPopup(s: Sede): HTMLElement {
    const div = document.createElement('div');
    const nombre = document.createElement('strong');
    nombre.textContent = s.nameVenue;
    div.append(nombre, document.createElement('br'), s.addressVenue ?? '');
    return div;
  }

  private pintarMarcadores(): void {
    if (!this.mapa) return;
    this.marcadores.forEach(m => m.remove());
    this.marcadores.clear();

    const conUbicacion = this.mostradas().filter(s => s.latitude != null && s.longitude != null);
    for (const s of conUbicacion) {
      const marcador = L.marker([s.latitude!, s.longitude!], { icon: this.icono, title: s.nameVenue })
        .bindPopup(this.contenidoPopup(s))
        .on('click', () => this.elegida.set(s.idVenue))
        .addTo(this.mapa);
      this.marcadores.set(s.idVenue, marcador);
    }
    if (conUbicacion.length) {
      const limites = L.latLngBounds(conUbicacion.map(s => [s.latitude!, s.longitude!] as L.LatLngTuple));
      this.mapa.fitBounds(limites, { padding: [48, 48], maxZoom: 14 });
    }
  }
}
