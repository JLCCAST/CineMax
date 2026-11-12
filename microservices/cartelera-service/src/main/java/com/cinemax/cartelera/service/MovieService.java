package com.cinemax.cartelera.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cinemax.cartelera.dto.ClassificationResponseDTO;
import com.cinemax.cartelera.dto.GenreResponseDTO;
import com.cinemax.cartelera.dto.MovieRequestDTO;
import com.cinemax.cartelera.dto.MovieResponseDTO;
import com.cinemax.cartelera.entity.Classification;
import com.cinemax.cartelera.entity.Genre;
import com.cinemax.cartelera.entity.Movie;
import com.cinemax.cartelera.repository.ClassificationRepository;
import com.cinemax.cartelera.repository.GenreRepository;
import com.cinemax.cartelera.repository.MovieRepository;

@Service
public class MovieService {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private ClassificationRepository classificationRepository;

    public List<MovieResponseDTO> findAll() {
        return movieRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<MovieResponseDTO> findByStatus(String status) {
        return movieRepository.findByStatus(status)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public MovieResponseDTO findById(String id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Película no encontrada con id: " + id));
        return toResponseDTO(movie);
    }

    public MovieResponseDTO create(MovieRequestDTO dto) {
        if (movieRepository.existsByTitleMovie(dto.getTitleMovie())) {
            throw new RuntimeException("Ya existe una película con ese título");
        }

        // Validamos que la clasificación exista antes de guardar
        Classification classification = classificationRepository.findById(dto.getClassificationId())
                .orElseThrow(() -> new RuntimeException("Clasificación no encontrada con id: " + dto.getClassificationId()));

        // Validamos que todos los géneros existan
        List<String> genreIds = dto.getGenreIds();
        if (genreIds != null) {
            for (String genreId : genreIds) {
                if (!genreRepository.existsById(genreId)) {
                    throw new RuntimeException("Género no encontrado con id: " + genreId);
                }
            }
        }

        Movie movie = Movie.builder()
                .titleMovie(dto.getTitleMovie())
                .synopsis(dto.getSynopsis())
                .durationMinutes(dto.getDurationMinutes())
                .posterUrl(dto.getPosterUrl())
                .releaseDate(dto.getReleaseDate())
                .status(dto.getStatus() != null ? dto.getStatus() : "Cartelera")
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .premiereWeek(dto.getPremiereWeek() != null ? dto.getPremiereWeek() : false)
                .director(dto.getDirector())
                .genreIds(genreIds)
                .classificationId(dto.getClassificationId())
                .build();

        return toResponseDTO(movieRepository.save(movie));
    }

    public MovieResponseDTO update(String id, MovieRequestDTO dto) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Película no encontrada con id: " + id));

        // Validamos clasificación si viene en el request
        if (dto.getClassificationId() != null) {
            classificationRepository.findById(dto.getClassificationId())
                    .orElseThrow(() -> new RuntimeException("Clasificación no encontrada con id: " + dto.getClassificationId()));
            movie.setClassificationId(dto.getClassificationId());
        }

        // Validamos géneros si vienen en el request
        if (dto.getGenreIds() != null) {
            for (String genreId : dto.getGenreIds()) {
                if (!genreRepository.existsById(genreId)) {
                    throw new RuntimeException("Género no encontrado con id: " + genreId);
                }
            }
            movie.setGenreIds(dto.getGenreIds());
        }

        movie.setTitleMovie(dto.getTitleMovie());
        movie.setSynopsis(dto.getSynopsis());
        movie.setDurationMinutes(dto.getDurationMinutes());
        // Sin póster nuevo se conserva el actual (el admin edita sin volver a subirlo).
        if (dto.getPosterUrl() != null && !dto.getPosterUrl().isBlank()) {
            movie.setPosterUrl(dto.getPosterUrl());
        }
        movie.setReleaseDate(dto.getReleaseDate());
        movie.setDirector(dto.getDirector());

        if (dto.getStatus() != null) {
            movie.setStatus(dto.getStatus());
        }
        if (dto.getIsActive() != null) {
            movie.setIsActive(dto.getIsActive());
        }
        if (dto.getPremiereWeek() != null) {
            movie.setPremiereWeek(dto.getPremiereWeek());
        }

        return toResponseDTO(movieRepository.save(movie));
    }

    public void delete(String id) {
        if (!movieRepository.existsById(id)) {
            throw new RuntimeException("Película no encontrada con id: " + id);
        }
        movieRepository.deleteById(id);
    }

    private MovieResponseDTO toResponseDTO(Movie movie) {
        // Traemos la info completa de la clasificación
        ClassificationResponseDTO classificationDTO = null;
        if (movie.getClassificationId() != null) {
            classificationDTO = classificationRepository.findById(movie.getClassificationId())
                    .map(c -> ClassificationResponseDTO.builder()
                            .idClassification(c.getIdClassification())
                            .nameClassification(c.getNameClassification())
                            .descriptionText(c.getDescriptionText())
                            .build())
                    .orElse(null);
        }

        // Traemos la info completa de cada género
        List<GenreResponseDTO> genreDTOs = List.of();
        if (movie.getGenreIds() != null) {
            genreDTOs = movie.getGenreIds().stream()
                    .map(genreRepository::findById)
                    .filter(java.util.Optional::isPresent)
                    .map(java.util.Optional::get)
                    .map(g -> GenreResponseDTO.builder()
                            .idGenre(g.getIdGenre())
                            .nameGenre(g.getNameGenre())
                            .build())
                    .toList();
        }

        return MovieResponseDTO.builder()
                .idMovie(movie.getIdMovie())
                .titleMovie(movie.getTitleMovie())
                .synopsis(movie.getSynopsis())
                .durationMinutes(movie.getDurationMinutes())
                .posterUrl(movie.getPosterUrl())
                .releaseDate(movie.getReleaseDate())
                .status(movie.getStatus())
                .createdAt(movie.getCreatedAt())
                .isActive(movie.getIsActive())
                .premiereWeek(movie.getPremiereWeek())
                .director(movie.getDirector())
                .genres(genreDTOs)
                .classification(classificationDTO)
                .build();
    }
}