package com.cinemax.cartelera.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TarifasTest {

    @ParameterizedTest(name = "base {0}: niño {1}, reducida {2}")
    @CsvSource({
            // base, niño (55 %), adulto mayor y discapacitado (65 %), redondeado a S/ 0,50
            "25.00, 14.00, 16.50",
            "20.00, 11.00, 13.00",
            "15.00,  8.50, 10.00",
            "30.00, 16.50, 19.50",
            "18.00, 10.00, 11.50",
            "10.00,  5.50,  6.50",
            " 0.00,  0.00,  0.00",
    })
    void cada_entrada_es_un_porcentaje_del_precio_base(String base, String nino, String reducida) {
        assertEquals(new BigDecimal(nino), Tarifas.aplicar(new BigDecimal(base), Tarifas.NINO));
        assertEquals(new BigDecimal(reducida), Tarifas.aplicar(new BigDecimal(base), Tarifas.REDUCIDA));
    }
}
