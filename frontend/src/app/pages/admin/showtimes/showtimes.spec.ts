import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { AdminShowtimes } from './showtimes';
import { ShowtimeService } from '../../../services/showtime.service';
import { MovieService } from '../../../services/movie.service';
import { RoomService } from '../../../services/room.service';
import { VenueService } from '../../../services/venue.service';
import { AuthService } from '../../../services/auth.service';

/**
 * Pruebas unitarias del componente AdminShowtimes (HU-14).
 *
 * Estrategia: TestBed mínimo con mocks de los 5 servicios.
 * Se prueban:
 *   - Inicialización del FormGroup (campos y validadores)
 *   - Validaciones: required, min/max, fechaNoPassadaValidator, horaNoPassadaValidator
 *   - Lógica de negocio: invalido(), calcularHoraFin(), precioNino/precioMayor
 *   - Filtros: funcionesFiltradas, formatos
 *   - Integración con servicios: cargarFunciones(), guardarFuncion()
 *   - getBadgeClase()
 */
describe('AdminShowtimes (HU-14)', () => {

  let component: AdminShowtimes;
  let fixture: ComponentFixture<AdminShowtimes>;

  let mockShowtimeService: jasmine.SpyObj<ShowtimeService>;
  let mockMovieService: jasmine.SpyObj<MovieService>;
  let mockRoomService: jasmine.SpyObj<RoomService>;
  let mockVenueService: jasmine.SpyObj<VenueService>;
  let mockAuthService: jasmine.SpyObj<AuthService>;

  const HOY = new Date().toISOString().split('T')[0];
  const MANANA = new Date(Date.now() + 86400000).toISOString().split('T')[0];

  const peliculasMock = [
    { idMovie: 1, titleMovie: 'Inception',    durationMinutes: 148, status: 'Cartelera', synopsis: 'A thief leads a team of specialists', posterUrl: 'inception.jpg', releaseDate: '2010-07-16', director: 'Christopher Nolan', premiereWeek: false },
    { idMovie: 2, titleMovie: 'Dune Part Two', durationMinutes: 166, status: 'Cartelera', synopsis: 'The saga continues', posterUrl: 'dune2.jpg', releaseDate: '2024-02-28', director: 'Denis Villeneuve', premiereWeek: true },
  ];

  const salasMock = [
    { idRoom: 1, nameRoom: 'Sala A', status: 'Activo',   idVenue: 1 },
    { idRoom: 2, nameRoom: 'Sala B', status: 'Inactivo', idVenue: 1 },
  ];

  const funcionesMock = [
    { idShowtime: 10, idMovie: 1, idRoom: 1, titleMovie: 'Inception',    showDate: HOY, startTime: '14:00', languageFormat: 'Doblada 2D',    baseTicketPrice: 15, status: 'Programada' },
    { idShowtime: 11, idMovie: 2, idRoom: 2, titleMovie: 'Dune Part Two', showDate: HOY, startTime: '18:00', languageFormat: 'Subtitulada 2D', baseTicketPrice: 18, status: 'Finalizada' },
  ];

  beforeEach(async () => {
    mockShowtimeService = jasmine.createSpyObj('ShowtimeService', [
      'getShowtimesByVenue', 'createShowtime', 'updateShowtime', 'cancelShowtime'
    ]);
    mockMovieService  = jasmine.createSpyObj('MovieService',  ['getMoviesByStatus']);
    mockRoomService   = jasmine.createSpyObj('RoomService',   ['getRoomsByVenue']);
    mockVenueService  = jasmine.createSpyObj('VenueService',  ['getVenues']);
    mockAuthService   = jasmine.createSpyObj('AuthService',   ['getRole', 'getIdVenue']);

    // Por defecto: gerente de sede (no global) con idVenue = 1
    mockAuthService.getRole.and.returnValue('ROLE_GERENTE_DE_OPERACIONES');
    mockAuthService.getIdVenue.and.returnValue(1);
    mockMovieService.getMoviesByStatus.and.returnValue(of(peliculasMock));
    mockRoomService.getRoomsByVenue.and.returnValue(of(salasMock));
    mockShowtimeService.getShowtimesByVenue.and.returnValue(of(funcionesMock));
    mockVenueService.getVenues.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [AdminShowtimes, ReactiveFormsModule],
      providers: [
        FormBuilder,
        { provide: ShowtimeService, useValue: mockShowtimeService },
        { provide: MovieService,    useValue: mockMovieService },
        { provide: RoomService,     useValue: mockRoomService },
        { provide: VenueService,    useValue: mockVenueService },
        { provide: AuthService,     useValue: mockAuthService },
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminShowtimes);
    component = fixture.componentInstance;
    fixture.detectChanges(); // dispara ngOnInit
  });

  // ── Inicialización ─────────────────────────────────────────────────────────

  describe('Inicialización', () => {

    it('debe crear el componente correctamente', () => {
      expect(component).toBeTruthy();
    });

    it('debe llamar a getMoviesByStatus y getRoomsByVenue al inicializarse', () => {
      expect(mockMovieService.getMoviesByStatus).toHaveBeenCalledWith('Cartelera');
      expect(mockRoomService.getRoomsByVenue).toHaveBeenCalledWith(1);
    });

    it('debe construir el FormGroup con todos los campos esperados', () => {
      expect(component.form).toBeTruthy();
      ['idMovie', 'idRoom', 'showDate', 'startTime', 'languageFormat', 'baseTicketPrice'].forEach(campo => {
        expect(component.form.contains(campo)).toBeTrue();
      });
    });

    it('filtroFecha se inicializa con la fecha de hoy', () => {
      expect(component.filtroFecha).toBe(HOY);
    });

    it('gerente de sede: esGerGeneral es false y carga salas directamente', () => {
      expect(component.esGerGeneral).toBeFalse();
      expect(mockRoomService.getRoomsByVenue).toHaveBeenCalledWith(1);
    });

    it('rooms solo contiene salas con status Activo tras cargarSalas()', () => {
      // salasMock tiene 1 Activo y 1 Inactivo
      expect(component.rooms.length).toBe(1);
      expect(component.rooms[0].nameRoom).toBe('Sala A');
    });
  });

  // ── Validaciones del FormGroup ─────────────────────────────────────────────

  describe('FormGroup — validaciones', () => {

    beforeEach(() => {
      component.abrirModalNuevo();
    });

    it('CP-FUN-01: idMovie en null hace el campo inválido (required)', () => {
      component.form.patchValue({ idMovie: null });
      expect(component.form.get('idMovie')?.hasError('required')).toBeTrue();
    });

    it('CP-FUN-02: idRoom en null hace el campo inválido (required)', () => {
      component.form.patchValue({ idRoom: null });
      expect(component.form.get('idRoom')?.hasError('required')).toBeTrue();
    });

    it('CP-FUN-03: showDate vacío hace el campo inválido (required)', () => {
      component.form.patchValue({ showDate: '' });
      expect(component.form.get('showDate')?.hasError('required')).toBeTrue();
    });

    it('showDate con fecha pasada dispara el error fechaPasada', () => {
      component.form.patchValue({ showDate: '2020-01-01' });
      expect(component.form.get('showDate')?.hasError('fechaPasada')).toBeTrue();
    });

    it('showDate con fecha futura es válida', () => {
      component.form.patchValue({ showDate: MANANA });
      expect(component.form.get('showDate')?.hasError('fechaPasada')).toBeFalse();
      expect(component.form.get('showDate')?.hasError('required')).toBeFalse();
    });

    it('CP-FUN-04: startTime vacío hace el campo inválido (required)', () => {
      component.form.patchValue({ startTime: '' });
      expect(component.form.get('startTime')?.hasError('required')).toBeTrue();
    });

    it('languageFormat vacío hace el campo inválido (required)', () => {
      component.form.patchValue({ languageFormat: '' });
      expect(component.form.get('languageFormat')?.hasError('required')).toBeTrue();
    });

    it('CP-FUN-05: baseTicketPrice en 0 viola min(1)', () => {
      component.form.patchValue({ baseTicketPrice: 0 });
      expect(component.form.get('baseTicketPrice')?.hasError('min')).toBeTrue();
    });

    it('baseTicketPrice en 1000 viola max(999)', () => {
      component.form.patchValue({ baseTicketPrice: 1000 });
      expect(component.form.get('baseTicketPrice')?.hasError('max')).toBeTrue();
    });

    it('formulario completo con datos válidos para fecha futura queda válido', () => {
      component.form.patchValue({
        idMovie: 1, idRoom: 1, showDate: MANANA,
        startTime: '15:00', languageFormat: 'Doblada 2D', baseTicketPrice: 15
      });
      expect(component.form.valid).toBeTrue();
    });
  });

  // ── horaNoPassadaValidator ─────────────────────────────────────────────────

  describe('horaNoPassadaValidator', () => {

    it('no aplica si la fecha NO es hoy (fecha futura = sin error horaPassada)', () => {
      component.form.patchValue({ showDate: MANANA, startTime: '00:01' });
      expect(component.form.get('startTime')?.hasError('horaPassada')).toBeFalse();
    });

    it('aplica si la fecha es hoy y la hora ingresada ya pasó', () => {
      // Forzar showDate = HOY y una hora que definitivamente pasó (00:01)
      component.form.patchValue({ showDate: HOY, startTime: '00:01' });
      component.form.get('startTime')!.updateValueAndValidity();
      expect(component.form.get('startTime')?.hasError('horaPassada')).toBeTrue();
    });

    it('no hay error si startTime está vacío (required lo maneja, no este validador)', () => {
      component.form.patchValue({ showDate: HOY, startTime: '' });
      component.form.get('startTime')!.updateValueAndValidity();
      expect(component.form.get('startTime')?.hasError('horaPassada')).toBeFalse();
    });
  });

  // ── invalido() ─────────────────────────────────────────────────────────────

  describe('invalido()', () => {

    it('devuelve false si el campo es válido y el formulario no se envió', () => {
      component.form.patchValue({ languageFormat: 'Doblada 2D' });
      expect(component.invalido('languageFormat')).toBeFalse();
    });

    it('devuelve true si el campo es inválido y formEnviado es true', () => {
      component.form.patchValue({ idMovie: null });
      component.formEnviado = true;
      expect(component.invalido('idMovie')).toBeTrue();
    });

    it('devuelve true si el campo fue touched y tiene error', () => {
      component.form.get('idMovie')?.markAsTouched();
      component.form.patchValue({ idMovie: null });
      expect(component.invalido('idMovie')).toBeTrue();
    });
  });

  // ── calcularHoraFin() ──────────────────────────────────────────────────────

  describe('calcularHoraFin()', () => {

    beforeEach(() => {
      component.movies = peliculasMock as any;
    });

    it('calcula la hora fin correctamente (duración + 30 min de limpieza)', () => {
      // Inception: 148 min. Inicio 14:00 → 14:00 + 148 + 30 = 16:58
      component.form.patchValue({ idMovie: 1, startTime: '14:00' });
      component.calcularHoraFin();
      expect(component.horaFinEstimada).toBe('16:58');
    });

    it('maneja el cruce de medianoche correctamente', () => {
      // Inception 148 min. Inicio 22:00 → 22:00 + 178 min = 00:58
      component.form.patchValue({ idMovie: 1, startTime: '22:00' });
      component.calcularHoraFin();
      expect(component.horaFinEstimada).toBe('00:58');
    });

    it('limpia horaFinEstimada si no hay película seleccionada', () => {
      component.form.patchValue({ idMovie: null, startTime: '15:00' });
      component.calcularHoraFin();
      expect(component.horaFinEstimada).toBe('');
    });

    it('limpia horaFinEstimada si no hay hora de inicio', () => {
      component.form.patchValue({ idMovie: 1, startTime: '' });
      component.calcularHoraFin();
      expect(component.horaFinEstimada).toBe('');
    });
  });

  // ── precioNino / precioMayor ───────────────────────────────────────────────

  describe('precioNino y precioMayor', () => {

    it('precioNino es el 55 % del precio base, redondeado a S/ 0,50', () => {
      component.form.patchValue({ baseTicketPrice: 25 });
      expect(component.precioNino).toBe(14);
    });

    it('precioMayor es el 65 % del precio base, redondeado a S/ 0,50', () => {
      component.form.patchValue({ baseTicketPrice: 25 });
      expect(component.precioMayor).toBe(16.5);
    });

    it('con un precio base bajo el descuento sigue siendo proporcional', () => {
      component.form.patchValue({ baseTicketPrice: 10 });
      expect(component.precioNino).toBe(5.5);
      expect(component.precioMayor).toBe(6.5);
    });
  });

  // ── funcionesFiltradas ─────────────────────────────────────────────────────

  describe('funcionesFiltradas', () => {

    beforeEach(() => {
      component.showtimes = funcionesMock as any;
      component.filtroSala = null;
      component.filtroFormato = '';
    });

    it('sin filtros devuelve todas las funciones', () => {
      expect(component.funcionesFiltradas.length).toBe(2);
    });

    it('filtra por sala correctamente', () => {
      component.filtroSala = 1;
      expect(component.funcionesFiltradas.length).toBe(1);
      expect(component.funcionesFiltradas[0].idRoom).toBe(1);
    });

    it('filtra por formato correctamente', () => {
      component.filtroFormato = 'Subtitulada 2D';
      expect(component.funcionesFiltradas.length).toBe(1);
      expect(component.funcionesFiltradas[0].languageFormat).toBe('Subtitulada 2D');
    });

    it('combina filtro de sala y formato', () => {
      component.filtroSala = 1;
      component.filtroFormato = 'Subtitulada 2D';
      expect(component.funcionesFiltradas.length).toBe(0);
    });
  });

  // ── formatos ───────────────────────────────────────────────────────────────

  describe('formatos', () => {

    it('devuelve la lista de formatos únicos de las funciones cargadas', () => {
      component.showtimes = funcionesMock as any;
      expect(component.formatos.length).toBe(2);
      expect(component.formatos).toContain('Doblada 2D');
      expect(component.formatos).toContain('Subtitulada 2D');
    });
  });

  // ── getBadgeClase() ────────────────────────────────────────────────────────

  describe('getBadgeClase()', () => {

    it('devuelve badge bg-success para Programada', () => {
      expect(component.getBadgeClase('Programada')).toBe('badge bg-success');
    });

    it('devuelve badge bg-warning text-dark para En Curso', () => {
      expect(component.getBadgeClase('En Curso')).toBe('badge bg-warning text-dark');
    });

    it('devuelve badge bg-secondary para Finalizada', () => {
      expect(component.getBadgeClase('Finalizada')).toBe('badge bg-secondary');
    });

    it('devuelve badge bg-danger para Cancelada', () => {
      expect(component.getBadgeClase('Cancelada')).toBe('badge bg-danger');
    });

    it('devuelve badge bg-light text-dark para un status desconocido', () => {
      expect(component.getBadgeClase('Desconocido')).toBe('badge bg-light text-dark');
    });
  });

  // ── guardarFuncion() ───────────────────────────────────────────────────────

  describe('guardarFuncion()', () => {

    beforeEach(() => {
      component.abrirModalNuevo();
    });

    it('no llama al servicio si el formulario es inválido', () => {
      component.guardarFuncion();
      expect(mockShowtimeService.createShowtime).not.toHaveBeenCalled();
    });

    it('activa formEnviado = true al intentar guardar con formulario inválido', () => {
      component.formEnviado = false;
      component.guardarFuncion();
      expect(component.formEnviado).toBeTrue();
    });

    it('llama a createShowtime en modo creación con formulario válido', () => {
      mockShowtimeService.createShowtime.and.returnValue(of({}));
      component.form.patchValue({
        idMovie: 1, idRoom: 1, showDate: MANANA,
        startTime: '15:00', languageFormat: 'Doblada 2D', baseTicketPrice: 15
      });
      component.guardarFuncion();
      expect(mockShowtimeService.createShowtime).toHaveBeenCalledTimes(1);
    });

    it('llama a updateShowtime en modo edición con formulario válido', () => {
      mockShowtimeService.updateShowtime.and.returnValue(of({}));
      component.isEditMode = true;
      component.currentShowtimeId = 10;
      component.form.patchValue({
        idMovie: 1, idRoom: 1, showDate: MANANA,
        startTime: '15:00', languageFormat: 'Doblada 2D', baseTicketPrice: 15
      });
      component.guardarFuncion();
      expect(mockShowtimeService.updateShowtime).toHaveBeenCalledWith(10, jasmine.any(Object));
    });

    it('asigna mensajeError si el servicio devuelve un error', () => {
      mockShowtimeService.createShowtime.and.returnValue(
        throwError(() => ({ error: { message: 'Conflicto de horario en la sala.' } }))
      );
      component.form.patchValue({
        idMovie: 1, idRoom: 1, showDate: MANANA,
        startTime: '15:00', languageFormat: 'Doblada 2D', baseTicketPrice: 15
      });
      component.guardarFuncion();
      expect(component.mensajeError).toContain('Conflicto de horario');
    });
  });

  // ── abrirModalNuevo() ──────────────────────────────────────────────────────

  describe('abrirModalNuevo()', () => {

    it('resetea isEditMode a false y currentShowtimeId a null', () => {
      component.isEditMode = true;
      component.currentShowtimeId = 99;
      component.abrirModalNuevo();
      expect(component.isEditMode).toBeFalse();
      expect(component.currentShowtimeId).toBeNull();
    });

    it('resetea formEnviado a false', () => {
      component.formEnviado = true;
      component.abrirModalNuevo();
      expect(component.formEnviado).toBeFalse();
    });

    it('limpia mensajeError y mensajeExito', () => {
      component.mensajeError = 'algo falló';
      component.mensajeExito = 'algo ok';
      component.abrirModalNuevo();
      expect(component.mensajeError).toBe('');
      expect(component.mensajeExito).toBe('');
    });

    it('resetea horaFinEstimada a cadena vacía', () => {
      component.horaFinEstimada = '18:30';
      component.abrirModalNuevo();
      expect(component.horaFinEstimada).toBe('');
    });
  });
});