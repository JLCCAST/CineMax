import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MovieService, Movie } from '../../../services/movie.service';
import { GenreService } from '../../../services/genre.service';
import { ClassificationService } from '../../../services/classification.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-movies',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './movies.html',
  styleUrls: ['./movies.css']
})
export class AdminMovies implements OnInit {

  movies: Movie[] = [];
  genres: any[] = [];
  classifications: any[] = [];

  cargando = true;
  filtroEstado = 'Cartelera';
  isEditMode = false;
  selectedFile: File | null = null;
  currentMovieId: string | null = null;
  peliculaDetalle: any = null;
  formEnviado = false;

  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private movieService: MovieService,
    private genreService: GenreService,
    private classificationService: ClassificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.construirForm();
    this.cargarCatalogos();
    this.cargarPeliculas();
  }

  // ── Formulario ────────────────────────────────────────────────────────────

  private construirForm(): void {
    this.form = this.fb.group({
      titleMovie:       ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      director:         ['', [Validators.required, Validators.maxLength(100)]],
      synopsis:         ['', Validators.maxLength(500)],
      durationMinutes:  [null, [Validators.required, Validators.min(1), Validators.max(300)]],
      releaseDate:      [''],   // opcional, sin restricción de fecha pasada (películas ya estrenadas)
      status:           ['Cartelera', Validators.required],
      premiereWeek:     [false],
      idClassification: [null, Validators.required],
      idGenres:         [[]]
    });
  }

  /** Acceso rápido a un control para usarlo en el template */
  ctrl(name: string) { return this.form.get(name)!; }

  /** ¿El control fue tocado y tiene error? */
  invalido(name: string): boolean {
    const c = this.ctrl(name);
    return c.invalid && (c.touched || this.formEnviado);
  }

  // ── Géneros (checkbox manual sobre el array del form) ─────────────────────

  toggleGenre(id: string): void {
    const actuales: string[] = this.ctrl('idGenres').value ?? [];
    const idx = actuales.indexOf(id);
    const nuevos = idx > -1
      ? actuales.filter(g => g !== id)
      : [...actuales, id];
    this.ctrl('idGenres').setValue(nuevos);
  }

  isGenreSelected(id: string): boolean {
    return (this.ctrl('idGenres').value ?? []).includes(id);
  }

  get sinGeneros(): boolean {
    return (this.ctrl('idGenres').value ?? []).length === 0;
  }

  // ── Datos ─────────────────────────────────────────────────────────────────

  cargarCatalogos(): void {
    this.classificationService.getAllClassifications().subscribe(res => {
      this.classifications = res;
      this.cdr.detectChanges();
    });
    this.genreService.getAllGenres().subscribe(res => {
      this.genres = res;
      this.cdr.detectChanges();
    });
  }

  cargarPeliculas(): void {
    this.cargando = true;
    this.movieService.getMoviesByStatus(this.filtroEstado).subscribe({
      next: res => { this.movies = res; this.cargando = false; this.cdr.detectChanges(); },
      error: ()  => { this.cargando = false; }
    });
  }

  onEstadoChange(): void { this.cargarPeliculas(); }

  // ── Modales ───────────────────────────────────────────────────────────────

  abrirModalNuevo(): void {
    this.isEditMode = false;
    this.currentMovieId = null;
    this.selectedFile = null;
    this.formEnviado = false;
    this.form.reset({
      titleMovie: '', director: '', synopsis: '',
      durationMinutes: null, releaseDate: '',
      status: this.filtroEstado, premiereWeek: false,
      idClassification: null, idGenres: []
    });
  }

  abrirModalEditar(peli: Movie): void {
    this.isEditMode = true;
    this.currentMovieId = peli.idMovie;
    this.selectedFile = null;
    this.formEnviado = false;
    this.form.reset({ titleMovie: 'Cargando...', idGenres: [] });

    this.movieService.getMovieById(peli.idMovie).subscribe(d => {
      const genreIds = (d.genres ?? []).map(g => g.idGenre);

      this.form.setValue({
        titleMovie:       d.titleMovie,
        director:         d.director,
        synopsis:         d.synopsis ?? '',
        durationMinutes:  d.durationMinutes,
        releaseDate:      d.releaseDate ?? '',
        status:           d.status ?? 'Cartelera',
        premiereWeek:     d.premiereWeek ?? false,
        idClassification: d.classification?.idClassification ?? null,
        idGenres:         genreIds
      });
      this.cdr.detectChanges();
    });
  }

  abrirModalDetalles(peli: Movie): void {
    this.peliculaDetalle = null;
    this.movieService.getMovieById(peli.idMovie).subscribe(d => {
      this.peliculaDetalle = d;
      this.cdr.detectChanges();
    });
  }

  onFileSelected(event: any): void {
    if (event.target.files.length > 0) this.selectedFile = event.target.files[0];
  }

  // ── Guardar ───────────────────────────────────────────────────────────────

  guardarPelicula(): void {
    this.formEnviado = true;
    this.form.markAllAsTouched();
    if (this.form.invalid || this.sinGeneros) return;
  
    // Mismos nombres que MovieRequestDTO de cartelera.
    const { idClassification, idGenres, releaseDate, ...resto } = this.form.value;
    const formData = new FormData();
    formData.append('movie', JSON.stringify({
      ...resto,
      releaseDate: releaseDate || null,
      classificationId: idClassification,
      genreIds: idGenres
    }));
    if (this.selectedFile) formData.append('file', this.selectedFile);

    const mensajeError = (err: any) => err.error?.message ?? 'No se pudo guardar la película.';
    if (this.isEditMode && this.currentMovieId) {
      this.movieService.updateMovie(this.currentMovieId, formData).subscribe({
        next: () => {
          this.cargarPeliculas();
          alert('Película actualizada correctamente');
          document.getElementById('btnCerrarModalPelicula')?.click();
        },
        error: err => alert('Error al actualizar: ' + mensajeError(err))
      });
    } else {
      this.movieService.createMovie(formData).subscribe({
        next: () => {
          this.cargarPeliculas();
          alert('Película creada exitosamente');
          document.getElementById('btnCerrarModalPelicula')?.click();
        },
        error: err => alert('Error al crear: ' + mensajeError(err))
      });
    }
  }

  inhabilitarPelicula(id: string): void {
    if (confirm('¿Estás seguro de inhabilitar esta película?')) {
      this.movieService.deleteMovie(id).subscribe(() => {
        this.cargarPeliculas();
        alert('Película inhabilitada');
      });
    }
  }
}