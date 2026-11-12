package com.cinemax.facturacion.promociones;

import com.cinemax.facturacion.model.entity.Promotion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Decide qué promoción aplica a una compra y cuánto descuenta en cada línea.
 *
 * No llama a otros servicios ni a la base: recibe todo lo que necesita. Se
 * aplica una sola promoción, la de mayor descuento. En un empate gana la que no
 * limita usos por cliente, luego la de mayor prioridad y luego la de menor id.
 */
public class PromotionEngine {

    public static final String ENTRADAS = "ENTRADAS";
    public static final String CONFITERIA = "CONFITERIA";

    private static final int DIAS_DE_ESTRENO = 7;
    private static final BigDecimal CIEN = new BigDecimal("100");

    public record Ticket(String fareCategory, BigDecimal price) {}

    public record Snack(Integer idSnack, Integer idCategory, BigDecimal unitPrice, int quantity) {
        public BigDecimal amount() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }

    /** Cliente con sesión iniciada; su fecha de nacimiento puede faltar. */
    public record Customer(LocalDate birthDate) {}

    public record Sale(LocalDate showDate, LocalTime startTime, LocalDate releaseDate,
                       Customer customer, String couponCode,
                       List<Ticket> tickets, List<Snack> snacks) {

        public BigDecimal subtotal() {
            return ticketsTotal().add(snacksTotal());
        }

        BigDecimal ticketsTotal() {
            return tickets.stream().map(Ticket::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal snacksTotal() {
            return snacks.stream().map(Snack::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    public record Result(Promotion promotion, BigDecimal discount,
                         List<BigDecimal> ticketDiscounts, List<BigDecimal> snackDiscounts) {

        public boolean applied() {
            return promotion != null;
        }
    }

    private record Candidate(Promotion promotion, BigDecimal discount) {}

    private static final Comparator<Candidate> MEJOR = Comparator
            .comparing(Candidate::discount)
            .thenComparing(c -> c.promotion().getMaxUsesPerUser() == null)
            .thenComparing(c -> valorODefecto(c.promotion().getPriority()))
            .thenComparing(c -> c.promotion().getIdPromotion(), Comparator.reverseOrder());

    /**
     * @param usesByPromotion usos de cada promoción por este cliente en el año;
     *                        vacío si no hay cliente
     */
    public Result evaluate(Sale sale, List<Promotion> promotions, Map<Integer, Long> usesByPromotion) {
        Optional<Candidate> mejor = promotions.stream()
                .filter(p -> aplica(p, sale, usesByPromotion))
                .map(p -> new Candidate(p, descuento(p, sale)))
                .filter(c -> c.discount().signum() > 0)
                .max(MEJOR);

        if (mejor.isEmpty()) {
            return new Result(null, BigDecimal.ZERO.setScale(2),
                    ceros(sale.tickets().size()), ceros(sale.snacks().size()));
        }
        return repartir(mejor.get(), sale);
    }

    // ---- Condiciones ----------------------------------------------------------

    private boolean aplica(Promotion p, Sale sale, Map<Integer, Long> usos) {
        LocalDate fecha = sale.showDate();
        if (!"Activo".equals(p.getStatus())
                || fecha.isBefore(p.getStartDate()) || fecha.isAfter(p.getEndDate())) {
            return false;
        }
        if (tieneTexto(p.getPromotionCode())
                && !p.getPromotionCode().equalsIgnoreCase(recortar(sale.couponCode()))) {
            return false;
        }
        if (tieneTexto(p.getDayOfWeek()) && !dias(p.getDayOfWeek()).contains(fecha.getDayOfWeek())) {
            return false;
        }
        if (p.getStartTimeFrom() != null && sale.startTime().isBefore(p.getStartTimeFrom())) {
            return false;
        }
        if (p.getStartTimeTo() != null && !sale.startTime().isBefore(p.getStartTimeTo())) {
            return false;
        }
        if (Boolean.TRUE.equals(p.getExcludePremiereMovies()) && esEstreno(sale)) {
            return false;
        }
        if (p.getMinimumTicketQuantity() != null && sale.tickets().size() < p.getMinimumTicketQuantity()) {
            return false;
        }
        if (p.getMinimumPurchaseAmount() != null && sale.subtotal().compareTo(p.getMinimumPurchaseAmount()) < 0) {
            return false;
        }
        if (p.getRequiredSnackCategoryId() != null && sale.snacks().stream()
                .noneMatch(s -> p.getRequiredSnackCategoryId().equals(s.idCategory()))) {
            return false;
        }
        if ("CUMPLEANOS".equals(p.getCustomerCondition()) && !esCumpleanos(sale)) {
            return false;
        }
        if (p.getMaxUsesPerUser() != null && (sale.customer() == null
                || usos.getOrDefault(p.getIdPromotion(), 0L) >= p.getMaxUsesPerUser())) {
            return false;
        }
        return true;
    }

    // Estreno: la función es dentro de los 7 días siguientes al estreno (o antes, en preventa).
    private static boolean esEstreno(Sale sale) {
        return sale.releaseDate() != null
                && ChronoUnit.DAYS.between(sale.releaseDate(), sale.showDate()) < DIAS_DE_ESTRENO;
    }

    // El 29 de febrero cuenta como 28 en los años no bisiestos.
    private static boolean esCumpleanos(Sale sale) {
        if (sale.customer() == null || sale.customer().birthDate() == null) {
            return false;
        }
        LocalDate nacimiento = sale.customer().birthDate();
        LocalDate fecha = sale.showDate();
        if (nacimiento.getMonth() == Month.FEBRUARY && nacimiento.getDayOfMonth() == 29 && !fecha.isLeapYear()) {
            return fecha.getMonth() == Month.FEBRUARY && fecha.getDayOfMonth() == 28;
        }
        return nacimiento.getMonth() == fecha.getMonth() && nacimiento.getDayOfMonth() == fecha.getDayOfMonth();
    }

    private static Set<DayOfWeek> dias(String isoDays) {
        return Arrays.stream(isoDays.split(","))
                .map(String::trim)
                .filter(d -> !d.isEmpty())
                .map(d -> DayOfWeek.of(Integer.parseInt(d)))
                .collect(Collectors.toSet());
    }

    // ---- Beneficio --------------------------------------------------------------

    private BigDecimal descuento(Promotion p, Sale sale) {
        BigDecimal alcance = totalEnAlcance(p, sale);
        String tipo = p.getPromotionType().getNamePromotionType();

        BigDecimal descuento = switch (tipo) {
            case "PORCENTAJE" -> alcance.multiply(valorODefecto(p.getDiscountPercentage())).divide(CIEN);
            case "MONTO_FIJO" -> valorODefecto(p.getDiscountFixedAmount());
            case "DOS_POR_UNO" -> aplicaAEntradas(p) ? dosPorUno(sale.tickets()) : BigDecimal.ZERO;
            case "ENTRADA_GRATIS" -> aplicaAEntradas(p) ? entradaMasCara(sale.tickets()) : BigDecimal.ZERO;
            default -> BigDecimal.ZERO;
        };

        if (p.getMaxDiscountAmount() != null) {
            descuento = descuento.min(p.getMaxDiscountAmount());
        }
        return descuento.min(alcance).setScale(2, RoundingMode.HALF_UP);
    }

    // En cada par de entradas, la más barata es gratis.
    private static BigDecimal dosPorUno(List<Ticket> tickets) {
        List<BigDecimal> precios = tickets.stream()
                .map(Ticket::price)
                .sorted(Comparator.reverseOrder())
                .toList();
        return IntStream.range(0, precios.size())
                .filter(i -> i % 2 == 1)
                .mapToObj(precios::get)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal entradaMasCara(List<Ticket> tickets) {
        return tickets.stream().map(Ticket::price).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    }

    private static boolean aplicaAEntradas(Promotion p) {
        return !CONFITERIA.equals(p.getAppliesTo());
    }

    private static boolean aplicaAConfiteria(Promotion p) {
        return !ENTRADAS.equals(p.getAppliesTo());
    }

    private static BigDecimal totalEnAlcance(Promotion p, Sale sale) {
        BigDecimal total = BigDecimal.ZERO;
        if (aplicaAEntradas(p)) {
            total = total.add(sale.ticketsTotal());
        }
        if (aplicaAConfiteria(p)) {
            total = total.add(sale.snacksTotal());
        }
        return total;
    }

    // ---- Reparto por línea --------------------------------------------------------

    // Proporcional al importe de cada línea del alcance; el residuo del redondeo
    // va a la línea de mayor importe, para que la suma sea exacta.
    private Result repartir(Candidate ganador, Sale sale) {
        Promotion p = ganador.promotion();
        BigDecimal descuento = ganador.discount();

        List<BigDecimal> importes = new ArrayList<>();
        sale.tickets().forEach(t -> importes.add(aplicaAEntradas(p) ? t.price() : BigDecimal.ZERO));
        sale.snacks().forEach(s -> importes.add(aplicaAConfiteria(p) ? s.amount() : BigDecimal.ZERO));

        BigDecimal total = importes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BigDecimal> partes = new ArrayList<>();
        for (BigDecimal importe : importes) {
            partes.add(descuento.multiply(importe).divide(total, 2, RoundingMode.DOWN));
        }

        BigDecimal residuo = descuento.subtract(partes.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        int mayor = IntStream.range(0, importes.size())
                .boxed()
                .max(Comparator.comparing(importes::get))
                .orElse(0);
        partes.set(mayor, partes.get(mayor).add(residuo));

        int entradas = sale.tickets().size();
        return new Result(p, descuento,
                List.copyOf(partes.subList(0, entradas)),
                List.copyOf(partes.subList(entradas, partes.size())));
    }

    // ---- Utilidades --------------------------------------------------------------

    private static List<BigDecimal> ceros(int n) {
        return java.util.Collections.nCopies(n, BigDecimal.ZERO.setScale(2));
    }

    private static boolean tieneTexto(String s) {
        return s != null && !s.isBlank();
    }

    private static String recortar(String s) {
        return s == null ? "" : s.trim();
    }

    private static BigDecimal valorODefecto(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static int valorODefecto(Integer valor) {
        return valor == null ? 0 : valor;
    }
}
