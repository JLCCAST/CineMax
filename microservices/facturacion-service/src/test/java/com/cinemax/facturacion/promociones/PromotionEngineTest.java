package com.cinemax.facturacion.promociones;

import com.cinemax.facturacion.model.entity.Promotion;
import com.cinemax.facturacion.model.entity.PromotionType;
import com.cinemax.facturacion.promociones.PromotionEngine.Customer;
import com.cinemax.facturacion.promociones.PromotionEngine.Result;
import com.cinemax.facturacion.promociones.PromotionEngine.Sale;
import com.cinemax.facturacion.promociones.PromotionEngine.Snack;
import com.cinemax.facturacion.promociones.PromotionEngine.Ticket;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las situaciones de la especificación de la épica 9. Precio base de la
 * función: S/ 20, así que adulto S/ 20, niño S/ 9 y reducida S/ 11.
 */
class PromotionEngineTest {

    private static final LocalDate MARTES = LocalDate.of(2026, 11, 17);
    private static final LocalDate MIERCOLES = LocalDate.of(2026, 11, 18);
    private static final LocalDate JUEVES = LocalDate.of(2026, 11, 19);
    private static final LocalDate VIERNES = LocalDate.of(2026, 11, 20);

    private static final Ticket ADULTO = new Ticket("ADULTO", money("20.00"));
    private static final Ticket NINO = new Ticket("NINO", money("9.00"));

    private static final int BEBIDAS = 1;
    private static final int COMBOS = 2;

    private final PromotionEngine engine = new PromotionEngine();
    private final List<Promotion> promociones = semilla();

    // ---- Las 17 situaciones -------------------------------------------------------

    @Test
    void miercoles_sin_estreno_aplica_miercoles_de_cine() {
        assertPromocion(evaluar(venta(MIERCOLES, "19:00", 30).entradas(ADULTO, ADULTO)), "Miércoles de cine", "20.00");
    }

    @Test
    void miercoles_con_estreno_no_aplica() {
        assertSinPromocion(evaluar(venta(MIERCOLES, "19:00", 3).entradas(ADULTO, ADULTO)));
    }

    @Test
    void martes_dos_adultos_aplica_dos_por_uno() {
        assertPromocion(evaluar(venta(MARTES, "20:00", 30).entradas(ADULTO, ADULTO)), "Martes 2x1", "20.00");
    }

    @Test
    void martes_adulto_y_nino_regala_la_mas_barata() {
        assertPromocion(evaluar(venta(MARTES, "20:00", 30).entradas(ADULTO, NINO)), "Martes 2x1", "9.00");
    }

    @Test
    void martes_una_entrada_no_aplica() {
        assertSinPromocion(evaluar(venta(MARTES, "20:00", 30).entradas(ADULTO)));
    }

    @Test
    void jueves_antes_de_las_15_aplica_matine() {
        assertPromocion(evaluar(venta(JUEVES, "13:00", 30).entradas(ADULTO, ADULTO)), "Matiné", "8.00");
    }

    @Test
    void miercoles_de_matine_gana_el_mayor_descuento() {
        assertPromocion(evaluar(venta(MIERCOLES, "13:00", 30).entradas(ADULTO, ADULTO)), "Miércoles de cine", "20.00");
    }

    @Test
    void dos_entradas_y_un_combo_descuentan_en_confiteria() {
        Result r = evaluar(venta(VIERNES, "20:00", 30).entradas(ADULTO, ADULTO).snack(COMBOS, "25.00", 1));
        assertPromocion(r, "Combo + película", "5.00");
        assertEquals(money("0.00"), r.ticketDiscounts().get(0));
        assertEquals(money("5.00"), r.snackDiscounts().get(0));
    }

    @Test
    void una_entrada_y_un_combo_no_aplica() {
        assertSinPromocion(evaluar(venta(VIERNES, "20:00", 30).entradas(ADULTO).snack(COMBOS, "25.00", 1)));
    }

    @Test
    void cumpleanos_regala_la_entrada_mas_cara() {
        Venta v = venta(VIERNES, "20:00", 30).entradas(ADULTO, ADULTO).cumple(VIERNES.withYear(2000));
        assertPromocion(evaluar(v), "Cumpleañero", "20.00");
    }

    @Test
    void cumpleanos_ya_usado_este_anio_no_aplica() {
        Venta v = venta(VIERNES, "20:00", 30).entradas(ADULTO, ADULTO).cumple(VIERNES.withYear(2000));
        assertSinPromocion(engine.evaluate(v.build(), promociones, Map.of(id("Cumpleañero"), 1L)));
    }

    @Test
    void empate_gana_la_que_no_gasta_un_uso_limitado() {
        Venta v = venta(MARTES, "20:00", 30).entradas(ADULTO, ADULTO).cumple(MARTES.withYear(1999));
        assertPromocion(evaluar(v), "Martes 2x1", "20.00");
    }

    @Test
    void cupon_con_subtotal_de_45() {
        Venta v = venta(VIERNES, "20:00", 30).entradas(ADULTO).snack(BEBIDAS, "12.50", 2).cupon("APP10");
        assertPromocion(evaluar(v), "Cupón de la app", "4.50");
    }

    @Test
    void cupon_respeta_su_tope() {
        Venta v = venta(VIERNES, "20:00", 30).cupon("app10");
        for (int i = 0; i < 10; i++) {
            v.entradas(ADULTO);
        }
        assertPromocion(evaluar(v), "Cupón de la app", "15.00");
    }

    @Test
    void cupon_bajo_la_compra_minima_no_aplica() {
        Venta v = venta(VIERNES, "20:00", 30).entradas(ADULTO).snack(BEBIDAS, "15.00", 1).cupon("APP10");
        assertSinPromocion(evaluar(v));
    }

    @Test
    void miercoles_gana_al_cupon() {
        Venta v = venta(MIERCOLES, "19:00", 30).entradas(ADULTO, ADULTO).snack(BEBIDAS, "15.00", 2).cupon("APP10");
        assertPromocion(evaluar(v), "Miércoles de cine", "20.00");
    }

    @Test
    void la_suma_por_linea_es_igual_al_descuento_total() {
        Venta v = venta(VIERNES, "20:00", 30).entradas(ADULTO, NINO).snack(BEBIDAS, "8.50", 3).cupon("APP10");
        Result r = evaluar(v);
        assertTrue(r.applied());
        BigDecimal suma = r.ticketDiscounts().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(r.snackDiscounts().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(r.discount(), suma);
    }

    // ---- Casos borde --------------------------------------------------------------

    @Test
    void sin_sesion_no_aplica_el_cumpleanero() {
        Venta v = venta(VIERNES, "20:00", 30).entradas(ADULTO, ADULTO);
        assertSinPromocion(evaluar(v));
    }

    @Test
    void el_29_de_febrero_se_celebra_el_28_en_anio_no_bisiesto() {
        LocalDate dia28 = LocalDate.of(2027, 2, 28);
        Promotion cumple = buscar("Cumpleañero");
        cumple.setEndDate(LocalDate.of(2027, 12, 31));
        Venta v = venta(dia28, "20:00", 30).entradas(ADULTO).cumple(LocalDate.of(2000, 2, 29));
        assertPromocion(engine.evaluate(v.build(), List.of(cumple), Map.of()), "Cumpleañero", "20.00");
    }

    @Test
    void fuera_de_vigencia_no_aplica() {
        Venta v = venta(LocalDate.of(2027, 1, 6), "19:00", 30).entradas(ADULTO, ADULTO);
        assertSinPromocion(evaluar(v));
    }

    // ---- Apoyo ------------------------------------------------------------------------

    private Result evaluar(Venta v) {
        return engine.evaluate(v.build(), promociones, Map.of());
    }

    private void assertPromocion(Result r, String nombre, String descuento) {
        assertTrue(r.applied(), "Se esperaba " + nombre);
        assertEquals(nombre, r.promotion().getNamePromotion());
        assertEquals(money(descuento), r.discount());
    }

    private static void assertSinPromocion(Result r) {
        assertFalse(r.applied(), () -> "No se esperaba promoción y aplicó " + r.promotion().getNamePromotion());
        assertEquals(money("0.00"), r.discount());
    }

    private static BigDecimal money(String valor) {
        return new BigDecimal(valor);
    }

    private int id(String nombre) {
        return buscar(nombre).getIdPromotion();
    }

    private Promotion buscar(String nombre) {
        return promociones.stream().filter(p -> p.getNamePromotion().equals(nombre)).findFirst().orElseThrow();
    }

    private static Venta venta(LocalDate fecha, String hora, int diasDesdeEstreno) {
        return new Venta(fecha, LocalTime.parse(hora), fecha.minusDays(diasDesdeEstreno));
    }

    private static final class Venta {
        private final LocalDate fecha;
        private final LocalTime hora;
        private final LocalDate estreno;
        private final List<Ticket> entradas = new ArrayList<>();
        private final List<Snack> snacks = new ArrayList<>();
        private Customer cliente;
        private String cupon;

        Venta(LocalDate fecha, LocalTime hora, LocalDate estreno) {
            this.fecha = fecha;
            this.hora = hora;
            this.estreno = estreno;
        }

        Venta entradas(Ticket... tickets) {
            Collections.addAll(entradas, tickets);
            return this;
        }

        Venta snack(int categoria, String precio, int cantidad) {
            snacks.add(new Snack(snacks.size() + 1, categoria, money(precio), cantidad));
            return this;
        }

        Venta cumple(LocalDate nacimiento) {
            cliente = new Customer(nacimiento);
            return this;
        }

        Venta cupon(String codigo) {
            cupon = codigo;
            return this;
        }

        Sale build() {
            return new Sale(fecha, hora, estreno, cliente, cupon, entradas, snacks);
        }
    }

    /** Las mismas 6 promociones de V5__promociones_semilla.sql. */
    private static List<Promotion> semilla() {
        return List.of(
                promo(1, "Miércoles de cine", "PORCENTAJE", "ENTRADAS", p -> {
                    p.setDiscountPercentage(money("50.00"));
                    p.setDayOfWeek("3");
                    p.setExcludePremiereMovies(true);
                }),
                promo(2, "Martes 2x1", "DOS_POR_UNO", "ENTRADAS", p -> {
                    p.setDayOfWeek("2");
                    p.setMinimumTicketQuantity(2);
                }),
                promo(3, "Matiné", "PORCENTAJE", "ENTRADAS", p -> {
                    p.setDiscountPercentage(money("20.00"));
                    p.setStartTimeTo(LocalTime.of(15, 0));
                }),
                promo(4, "Combo + película", "MONTO_FIJO", "CONFITERIA", p -> {
                    p.setDiscountFixedAmount(money("5.00"));
                    p.setMinimumTicketQuantity(2);
                    p.setRequiredSnackCategoryId(COMBOS);
                }),
                promo(5, "Cumpleañero", "ENTRADA_GRATIS", "ENTRADAS", p -> {
                    p.setMaxDiscountAmount(money("25.00"));
                    p.setCustomerCondition("CUMPLEANOS");
                    p.setMaxUsesPerUser(1);
                }),
                promo(6, "Cupón de la app", "PORCENTAJE", "TODO", p -> {
                    p.setPromotionCode("APP10");
                    p.setDiscountPercentage(money("10.00"));
                    p.setMaxDiscountAmount(money("15.00"));
                    p.setMinimumPurchaseAmount(money("40.00"));
                }));
    }

    private static Promotion promo(int id, String nombre, String tipo, String alcance,
                                   java.util.function.Consumer<Promotion> ajustes) {
        Promotion p = Promotion.builder()
                .idPromotion(id)
                .namePromotion(nombre)
                .promotionType(PromotionType.builder().namePromotionType(tipo).build())
                .appliesTo(alcance)
                .startDate(LocalDate.of(2025, 7, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .minimumPurchaseAmount(BigDecimal.ZERO)
                .minimumTicketQuantity(1)
                .excludePremiereMovies(false)
                .status("Activo")
                .priority(0)
                .build();
        ajustes.accept(p);
        return p;
    }
}
