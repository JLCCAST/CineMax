import { Component, input } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Movie } from '../../../services/movie.service';

// Póster con su botón Comprar, para la portada y la cartelera.
@Component({
  selector: 'app-tarjeta-pelicula',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './tarjeta-pelicula.html',
  styleUrl: './tarjeta-pelicula.css'
})
export class TarjetaPelicula {
  pelicula = input.required<Movie>();

  readonly posterGenerico = 'Imagenes/poster-generico.svg';

  datos(): string {
    const m = this.pelicula();
    const h = Math.floor(m.durationMinutes / 60);
    const min = m.durationMinutes % 60;
    const duracion = h ? `${h} h ${min} min` : `${min} min`;
    const generos = (m.genres ?? []).map(g => g.nameGenre).slice(0, 2).join(' · ');
    return generos ? `${duracion} · ${generos}` : duracion;
  }
}
