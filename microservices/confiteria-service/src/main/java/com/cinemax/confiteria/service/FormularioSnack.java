package com.cinemax.confiteria.service;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cinemax.common.cloudinary.CloudinaryService;
import com.cinemax.confiteria.dto.SnackRequestDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

// El admin manda el producto como formulario: los datos en JSON y, si hay, la foto.
// Se valida igual que el JSON de la API y la foto se sube a Cloudinary.
@Service
@RequiredArgsConstructor
public class FormularioSnack {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ObjectProvider<CloudinaryService> cloudinary;

    public SnackRequestDTO leer(String snackJson, MultipartFile foto) {
        SnackRequestDTO dto;
        try {
            dto = objectMapper.readValue(snackJson, SnackRequestDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Los datos del producto no tienen el formato esperado.");
        }
        Set<ConstraintViolation<SnackRequestDTO>> errores = validator.validate(dto);
        if (!errores.isEmpty()) {
            throw new RuntimeException(errores.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining(". ")));
        }
        if (foto != null && !foto.isEmpty()) {
            dto.setImageUrlSnack(subir(foto));
        }
        return dto;
    }

    private String subir(MultipartFile foto) {
        CloudinaryService servicio = cloudinary.getIfAvailable();
        if (servicio == null) {
            throw new RuntimeException("La subida de imágenes no está configurada: faltan las claves de Cloudinary.");
        }
        try {
            return servicio.uploadImage(foto);
        } catch (IOException | RuntimeException e) {
            throw new RuntimeException("No se pudo subir la foto. Revisa las claves de Cloudinary.");
        }
    }
}
