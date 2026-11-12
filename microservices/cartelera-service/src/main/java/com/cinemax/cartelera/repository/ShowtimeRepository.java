package com.cinemax.cartelera.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.cinemax.cartelera.entity.Showtime;

public interface ShowtimeRepository extends MongoRepository<Showtime, String> {

    List<Showtime> findByMovieId(String movieId);

    List<Showtime> findByShowDate(LocalDate showDate);

    List<Showtime> findByMovieIdAndShowDate(String movieId, LocalDate showDate);

    List<Showtime> findByIdVenueAndShowDate(Integer idVenue, LocalDate showDate);

    List<Showtime> findByIdRoom(Integer idRoom);
}
