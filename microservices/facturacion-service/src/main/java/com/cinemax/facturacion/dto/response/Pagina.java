package com.cinemax.facturacion.dto.response;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

// Una página de un listado. Los nombres siguen a Spring Data para que el frontend no traduzca.
public record Pagina<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static final int TAMANO_MAXIMO = 50;

    public static PageRequest pedido(int page, int size, Sort orden) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), TAMANO_MAXIMO), orden);
    }

    public static <T> Pagina<T> de(Page<?> pagina, List<T> contenido) {
        return new Pagina<>(contenido, pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
