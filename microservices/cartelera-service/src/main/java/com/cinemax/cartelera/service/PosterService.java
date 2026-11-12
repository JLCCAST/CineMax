package com.cinemax.cartelera.service;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cinemax.cartelera.dto.MovieRequestDTO;
import com.cinemax.common.cloudinary.CloudinaryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

/**
 * El admin de películas manda un formulario multipart: los datos en JSON y,
 * opcionalmente, el archivo del póster. Aquí se leen, se validan igual que el
 * JSON de la API y el póster se sube a Cloudinary.
 */
@Service
@RequiredArgsConstructor
public class PosterService {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ObjectProvider<CloudinaryService> cloudinary;

    public MovieRequestDTO leerFormulario(String movieJson, MultipartFile poster) {
        MovieRequestDTO dto = leer(movieJson);
        validar(dto);
        if (poster != null && !poster.isEmpty()) {
            dto.setPosterUrl(subir(poster));
        }
        return dto;
    }

    private MovieRequestDTO leer(String movieJson) {
        try {
            return objectMapper.readValue(movieJson, MovieRequestDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Los datos de la película no tienen el formato esperado.");
        }
    }

    private void validar(MovieRequestDTO dto) {
        Set<ConstraintViolation<MovieRequestDTO>> errores = validator.validate(dto);
        if (!errores.isEmpty()) {
            throw new RuntimeException(errores.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining(". ")));
        }
    }

    private String subir(MultipartFile poster) {
        CloudinaryService servicio = cloudinary.getIfAvailable();
        if (servicio == null) {
            throw new RuntimeException("La subida de imágenes no está configurada: faltan las claves de Cloudinary.");
        }
        try {
            return servicio.uploadImage(poster);
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException("No se pudo subir el póster. Revisa las claves de Cloudinary.");
        }
    }
}
