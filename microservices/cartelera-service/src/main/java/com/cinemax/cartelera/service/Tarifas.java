package com.cinemax.cartelera.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Precio de cada tipo de entrada a partir del precio base (el de adulto).
 *
 * Es un porcentaje fijo, así el descuento es proporcional a cualquier precio:
 * con un monto fijo (antes, base − 11 para niño) una función barata dejaba la
 * entrada de niño casi gratis y una cara apenas tenía descuento. El resultado
 * se redondea a S/ 0,50.
 */
public final class Tarifas {

    public static final BigDecimal NINO = new BigDecimal("0.55");
    public static final BigDecimal REDUCIDA = new BigDecimal("0.65");  // adulto mayor y discapacitado

    private static final BigDecimal DOS = BigDecimal.valueOf(2);

    private Tarifas() {
    }

    public static BigDecimal aplicar(BigDecimal base, BigDecimal porcentaje) {
        BigDecimal mediosSoles = base.multiply(porcentaje).multiply(DOS).setScale(0, RoundingMode.HALF_UP);
        return mediosSoles.divide(DOS, 2, RoundingMode.UNNECESSARY);
    }
}
