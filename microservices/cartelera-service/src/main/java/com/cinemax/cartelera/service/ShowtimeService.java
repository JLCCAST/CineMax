package com.cinemax.cartelera.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cinemax.cartelera.client.SucursalesClient;
import com.cinemax.cartelera.dto.ShowtimeRequestDTO;
import com.cinemax.cartelera.dto.ShowtimeResponseDTO;
import com.cinemax.cartelera.dto.ShowtimeSummaryDTO;
import com.cinemax.cartelera.dto.TicketFareDTO;
import com.cinemax.cartelera.dto.external.RoomVenueDTO;
import com.cinemax.cartelera.entity.Movie;
import com.cinemax.cartelera.entity.Showtime;
import com.cinemax.cartelera.repository.MovieRepository;
import com.cinemax.cartelera.repository.ShowtimeRepository;

import feign.FeignException;

@Service
public class ShowtimeService {

    private static final Logger log = LoggerFactory.getLogger(ShowtimeService.class);

    // Descuentos por categoría sobre el precio base (mismas reglas que el monolito).
    private static final BigDecimal DESCUENTO_NINO = new BigDecimal("11.00");
    private static final BigDecimal DESCUENTO_REDUCIDA = new BigDecimal("9.00");

    private static final Comparator<Showtime> POR_FECHA_Y_HORA = Comparator
            .comparing(Showtime::getShowDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Showtime::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()));

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private SucursalesClient sucursalesClient;

    public List<ShowtimeResponseDTO> findAll(String movieId, LocalDate showDate) {
        List<Showtime> showtimes;
        if (movieId != null && showDate != null) {
            showtimes = showtimeRepository.findByMovieIdAndShowDate(movieId, showDate);
        } else if (movieId != null) {
            showtimes = showtimeRepository.findByMovieId(movieId);
        } else if (showDate != null) {
            showtimes = showtimeRepository.findByShowDate(showDate);
        } else {
            showtimes = showtimeRepository.findAll();
        }
        return toResponseList(showtimes);
    }

    public List<ShowtimeResponseDTO> findByMovieId(String movieId) {
        return toResponseList(showtimeRepository.findByMovieId(movieId));
    }

    public List<ShowtimeResponseDTO> findByShowDate(LocalDate showDate) {
        return toResponseList(showtimeRepository.findByShowDate(showDate));
    }

    public List<ShowtimeResponseDTO> findByVenueAndDate(Integer idVenue, LocalDate showDate) {
        return toResponseList(showtimeRepository.findByIdVenueAndShowDate(idVenue, showDate));
    }

    public ShowtimeResponseDTO findById(String id) {
        return toResponseList(List.of(buscarFuncion(id))).get(0);
    }

    public ShowtimeSummaryDTO getSummary(String id) {
        Showtime showtime = buscarFuncion(id);
        Movie movie = movieRepository.findById(showtime.getMovieId()).orElse(null);
        RoomVenueDTO sala = buscarSalas(List.of(showtime)).get(showtime.getIdRoom());

        return ShowtimeSummaryDTO.builder()
                .idShowtime(showtime.getIdShowtime())
                .titleMovie(movie != null ? movie.getTitleMovie() : null)
                .posterUrl(movie != null ? movie.getPosterUrl() : null)
                .idRoom(showtime.getIdRoom())
                .nameRoom(sala != null ? sala.getNameRoom() : null)
                .idVenue(showtime.getIdVenue())
                .nameVenue(sala != null ? sala.getNameVenue() : null)
                .showDate(showtime.getShowDate())
                .startTime(showtime.getStartTime())
                .languageFormat(showtime.getLanguageFormat())
                .build();
    }

    public List<TicketFareDTO> getFares(String id) {
        BigDecimal base = buscarFuncion(id).getBaseTicketPrice();
        if (base == null) {
            throw new RuntimeException("La función no tiene precio base");
        }
        return List.of(
                new TicketFareDTO("ADULTO", "Adulto", base),
                new TicketFareDTO("NINO", "Niño", conDescuento(base, DESCUENTO_NINO)),
                new TicketFareDTO("ADULTO_MAYOR", "Adulto Mayor", conDescuento(base, DESCUENTO_REDUCIDA)),
                new TicketFareDTO("DISCAPACITADO", "Personas Discapacitadas", conDescuento(base, DESCUENTO_REDUCIDA)));
    }

    public ShowtimeResponseDTO create(ShowtimeRequestDTO dto) {
        movieRepository.findById(dto.getMovieId())
                .orElseThrow(() -> new RuntimeException("Película no encontrada con id: " + dto.getMovieId()));

        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new RuntimeException("La hora de fin no puede ser antes que la hora de inicio");
        }

        RoomVenueDTO sala = buscarSala(dto.getIdRoom());

        Showtime showtime = Showtime.builder()
                .showDate(dto.getShowDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .languageFormat(dto.getLanguageFormat())
                .baseTicketPrice(dto.getBaseTicketPrice())
                // Sin dato, la función empieza con toda la sala disponible.
                .availableSeats(dto.getAvailableSeats() != null ? dto.getAvailableSeats() : sala.getCapacity())
                .status(dto.getStatus() != null ? dto.getStatus() : "Programada")
                .movieId(dto.getMovieId())
                .idRoom(sala.getIdRoom())
                .idVenue(sala.getIdVenue())
                .build();

        return toResponseList(List.of(showtimeRepository.save(showtime))).get(0);
    }

    public ShowtimeResponseDTO update(String id, ShowtimeRequestDTO dto) {
        Showtime showtime = buscarFuncion(id);

        if (dto.getMovieId() != null) {
            movieRepository.findById(dto.getMovieId())
                    .orElseThrow(() -> new RuntimeException("Película no encontrada con id: " + dto.getMovieId()));
            showtime.setMovieId(dto.getMovieId());
        }

        if (dto.getEndTime() != null && dto.getStartTime() != null
                && dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new RuntimeException("La hora de fin no puede ser antes que la hora de inicio");
        }

        showtime.setShowDate(dto.getShowDate());
        showtime.setStartTime(dto.getStartTime());
        showtime.setEndTime(dto.getEndTime());
        showtime.setLanguageFormat(dto.getLanguageFormat());
        showtime.setBaseTicketPrice(dto.getBaseTicketPrice());
        if (dto.getAvailableSeats() != null) {
            showtime.setAvailableSeats(dto.getAvailableSeats());
        }

        if (!dto.getIdRoom().equals(showtime.getIdRoom()) || showtime.getIdVenue() == null) {
            RoomVenueDTO sala = buscarSala(dto.getIdRoom());
            showtime.setIdRoom(sala.getIdRoom());
            showtime.setIdVenue(sala.getIdVenue());
        }

        if (dto.getStatus() != null) {
            showtime.setStatus(dto.getStatus());
        }

        return toResponseList(List.of(showtimeRepository.save(showtime))).get(0);
    }

    public void delete(String id) {
        if (!showtimeRepository.existsById(id)) {
            throw new RuntimeException("Función no encontrada con id: " + id);
        }
        showtimeRepository.deleteById(id);
    }

    // Usado por facturacion-service al confirmar una venta, igual que hacía
    // el monolito: showtime.setAvailableSeats(availableSeats - seats.size()).
    public void decreaseAvailableSeats(String id, int cantidad) {
        Showtime showtime = buscarFuncion(id);
        showtime.setAvailableSeats(showtime.getAvailableSeats() - cantidad);
        showtimeRepository.save(showtime);
    }

    private Showtime buscarFuncion(String id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Función no encontrada con id: " + id));
    }

    private static BigDecimal conDescuento(BigDecimal base, BigDecimal descuento) {
        return base.subtract(descuento).max(BigDecimal.ZERO);
    }

    private RoomVenueDTO buscarSala(Integer idRoom) {
        try {
            return sucursalesClient.getRoomWithVenue(idRoom);
        } catch (FeignException.NotFound e) {
            throw new RuntimeException("Sala no encontrada con id: " + idRoom);
        }
    }

    // Nombres de sala y sede para una lista de funciones, en una sola llamada.
    // Si sucursales no responde, la lista se devuelve sin esos nombres.
    private Map<Integer, RoomVenueDTO> buscarSalas(List<Showtime> showtimes) {
        List<Integer> ids = showtimes.stream()
                .map(Showtime::getIdRoom)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        try {
            return sucursalesClient.getRoomsWithVenue(ids).stream()
                    .collect(Collectors.toMap(RoomVenueDTO::getIdRoom, Function.identity()));
        } catch (FeignException e) {
            log.warn("No se pudieron obtener las salas {} de sucursales-service: {}", ids, e.getMessage());
            return Map.of();
        }
    }

    private List<ShowtimeResponseDTO> toResponseList(List<Showtime> showtimes) {
        List<String> movieIds = showtimes.stream().map(Showtime::getMovieId).filter(Objects::nonNull).distinct().toList();
        Map<String, Movie> movies = movieRepository.findAllById(movieIds).stream()
                .collect(Collectors.toMap(Movie::getIdMovie, Function.identity()));
        Map<Integer, RoomVenueDTO> salas = buscarSalas(showtimes);

        return showtimes.stream()
                .sorted(POR_FECHA_Y_HORA)
                .map(s -> toResponseDTO(s, movies.get(s.getMovieId()), salas.get(s.getIdRoom())))
                .toList();
    }

    private ShowtimeResponseDTO toResponseDTO(Showtime showtime, Movie movie, RoomVenueDTO sala) {
        return ShowtimeResponseDTO.builder()
                .idShowtime(showtime.getIdShowtime())
                .showDate(showtime.getShowDate())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .languageFormat(showtime.getLanguageFormat())
                .baseTicketPrice(showtime.getBaseTicketPrice())
                .availableSeats(showtime.getAvailableSeats())
                .status(showtime.getStatus())
                .movieId(showtime.getMovieId())
                .movieTitle(movie != null ? movie.getTitleMovie() : null)
                .durationMinutes(movie != null ? movie.getDurationMinutes() : null)
                .idRoom(showtime.getIdRoom())
                .nameRoom(sala != null ? sala.getNameRoom() : null)
                .roomCapacity(sala != null ? sala.getCapacity() : null)
                .idVenue(showtime.getIdVenue())
                .nameVenue(sala != null ? sala.getNameVenue() : null)
                .build();
    }
}
