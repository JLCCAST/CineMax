package com.cinemax.facturacion.promociones;

import com.cinemax.facturacion.client.CarteleraClient;
import com.cinemax.facturacion.client.ConfiteriaClient;
import com.cinemax.facturacion.dto.external.SaleContextDTO;
import com.cinemax.facturacion.dto.external.SnackInfoDTO;
import com.cinemax.facturacion.dto.external.UserDTO;
import com.cinemax.facturacion.model.entity.Promotion;
import com.cinemax.facturacion.promociones.PromotionEngine.Customer;
import com.cinemax.facturacion.promociones.PromotionEngine.Result;
import com.cinemax.facturacion.promociones.PromotionEngine.Sale;
import com.cinemax.facturacion.promociones.PromotionEngine.Snack;
import com.cinemax.facturacion.promociones.PromotionEngine.Ticket;
import com.cinemax.facturacion.repository.PromotionRepository;
import com.cinemax.facturacion.repository.SaleTransactionRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Calcula cuánto cuesta una compra con precios del servidor: tarifas de
 * cartelera, precios vigentes de confitería y la promoción que aplique.
 * Lo usan la vista previa del checkout y la compra.
 */
@Service
@RequiredArgsConstructor
public class CotizacionService {

    private final CarteleraClient carteleraClient;
    private final ConfiteriaClient confiteriaClient;
    private final PromotionRepository promotionRepository;
    private final SaleTransactionRepository saleTransactionRepository;
    private final Clock reloj;
    private final PromotionEngine engine = new PromotionEngine();

    public record EntradaPedida(String categoryCode, int cantidad) {}

    public record SnackPedido(Integer idSnack, int cantidad) {}

    public record LineaEntrada(String categoryCode, String categoryName, BigDecimal price, BigDecimal discount) {}

    public record LineaSnack(Integer idSnack, String nameSnack, BigDecimal unitPrice, int quantity, BigDecimal discount) {}

    public record Cotizacion(SaleContextDTO contexto, List<LineaEntrada> entradas, List<LineaSnack> snacks,
                             Promotion promocion, BigDecimal subtotal, BigDecimal descuento, BigDecimal total) {}

    /**
     * @param idShowtime null en un pedido solo de confitería; entonces idVenue es la sede de recojo
     * @param cliente    null si no hay sesión iniciada
     */
    public Cotizacion cotizar(String idShowtime, Integer idVenue, List<EntradaPedida> entradas,
                              List<SnackPedido> snacks, String cupon, UserDTO cliente) {
        boolean soloConfiteria = idShowtime == null || idShowtime.isBlank();
        SaleContextDTO contexto = soloConfiteria
                ? contextoConfiteria(idVenue, entradas, snacks)
                : contextoFuncion(idShowtime);
        List<SaleContextDTO.Fare> tarifas = soloConfiteria ? List.of() : tarifaPorEntrada(contexto, entradas);
        List<SnackPedido> pedidos = snacks == null ? List.of()
                : snacks.stream().filter(s -> s.cantidad() > 0).toList();
        Map<Integer, SnackInfoDTO> catalogo = catalogo(pedidos);

        Sale venta = new Sale(
                contexto.getShowDate(),
                contexto.getStartTime(),
                contexto.getReleaseDate(),
                cliente != null ? new Customer(cliente.getDatebirth()) : null,
                cupon,
                tarifas.stream().map(t -> new Ticket(t.getCategoryCode(), t.getPrice())).toList(),
                pedidos.stream()
                        .map(p -> {
                            SnackInfoDTO s = catalogo.get(p.idSnack());
                            return new Snack(s.getIdSnack(), s.getIdSnackCategory(), s.getPrice(), p.cantidad());
                        })
                        .toList());

        List<Promotion> vigentes = promotionRepository.findByStatus("Activo");
        Result resultado = engine.evaluate(venta, vigentes, cliente != null ? usosDelAnio(cliente, vigentes) : Map.of());

        List<LineaEntrada> lineasEntrada = IntStream.range(0, tarifas.size())
                .mapToObj(i -> new LineaEntrada(tarifas.get(i).getCategoryCode(), tarifas.get(i).getCategoryName(),
                        tarifas.get(i).getPrice(), resultado.ticketDiscounts().get(i)))
                .toList();
        List<LineaSnack> lineasSnack = IntStream.range(0, pedidos.size())
                .mapToObj(i -> {
                    SnackInfoDTO s = catalogo.get(pedidos.get(i).idSnack());
                    return new LineaSnack(s.getIdSnack(), s.getNameSnack(), s.getPrice(),
                            pedidos.get(i).cantidad(), resultado.snackDiscounts().get(i));
                })
                .toList();

        BigDecimal subtotal = venta.subtotal().setScale(2);
        return new Cotizacion(contexto, lineasEntrada, lineasSnack, resultado.promotion(),
                subtotal, resultado.discount(), subtotal.subtract(resultado.discount()));
    }

    private SaleContextDTO contextoFuncion(String idShowtime) {
        SaleContextDTO contexto = contexto(idShowtime);
        if (!"Programada".equals(contexto.getStatus())) {
            throw new IllegalStateException("Esta función ya no está disponible.");
        }
        if (yaEmpezo(contexto)) {
            throw new IllegalStateException("Esta función ya empezó. Elige otro horario.");
        }
        return contexto;
    }

    // Pedido de confitería sin función: se recoge hoy en la sede elegida. Las
    // promociones de entradas no aplican; las de toda la compra (como un cupón) sí.
    private SaleContextDTO contextoConfiteria(Integer idVenue, List<EntradaPedida> entradas, List<SnackPedido> snacks) {
        if (idVenue == null) {
            throw new IllegalStateException("Selecciona una función o la sede donde recoges tu pedido.");
        }
        if (entradas != null && entradas.stream().anyMatch(e -> e.cantidad() > 0)) {
            throw new IllegalStateException("Para comprar entradas, selecciona una función.");
        }
        if (snacks == null || snacks.stream().noneMatch(s -> s.cantidad() > 0)) {
            throw new IllegalStateException("Elige al menos un producto.");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        SaleContextDTO contexto = new SaleContextDTO();
        contexto.setIdVenue(idVenue);
        contexto.setShowDate(ahora.toLocalDate());
        contexto.setStartTime(ahora.toLocalTime());
        contexto.setFares(List.of());
        return contexto;
    }

    private boolean yaEmpezo(SaleContextDTO contexto) {
        if (contexto.getShowDate() == null || contexto.getStartTime() == null) {
            return false;
        }
        return !LocalDateTime.of(contexto.getShowDate(), contexto.getStartTime()).isAfter(LocalDateTime.now(reloj));
    }

    private SaleContextDTO contexto(String idShowtime) {
        try {
            return carteleraClient.getSaleContext(idShowtime);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Función no encontrada.");
        }
    }

    // Una tarifa por entrada, en el orden en que se pidieron.
    private static List<SaleContextDTO.Fare> tarifaPorEntrada(SaleContextDTO contexto, List<EntradaPedida> entradas) {
        Map<String, SaleContextDTO.Fare> tarifas = contexto.getFares().stream()
                .collect(Collectors.toMap(SaleContextDTO.Fare::getCategoryCode, Function.identity()));
        List<SaleContextDTO.Fare> resultado = new ArrayList<>();
        for (EntradaPedida e : entradas == null ? List.<EntradaPedida>of() : entradas) {
            if (e.cantidad() <= 0) {
                continue;
            }
            SaleContextDTO.Fare tarifa = tarifas.get(e.categoryCode());
            if (tarifa == null) {
                throw new IllegalStateException("Tipo de entrada no válido: " + e.categoryCode());
            }
            for (int i = 0; i < e.cantidad(); i++) {
                resultado.add(tarifa);
            }
        }
        if (resultado.isEmpty()) {
            throw new IllegalStateException("Elige al menos una entrada.");
        }
        return resultado;
    }

    private Map<Integer, SnackInfoDTO> catalogo(List<SnackPedido> pedidos) {
        if (pedidos.isEmpty()) {
            return Map.of();
        }
        List<Integer> ids = pedidos.stream().map(SnackPedido::idSnack).distinct().toList();
        Map<Integer, SnackInfoDTO> catalogo = new HashMap<>();
        confiteriaClient.getSnacks(ids).forEach(s -> catalogo.put(s.getIdSnack(), s));
        for (SnackPedido p : pedidos) {
            SnackInfoDTO s = catalogo.get(p.idSnack());
            if (s == null || !"Activo".equals(s.getStatus())) {
                throw new IllegalStateException("El snack " + p.idSnack() + " no está disponible.");
            }
        }
        return catalogo;
    }

    // Solo cuentan las promociones con límite de usos.
    private Map<Integer, Long> usosDelAnio(UserDTO cliente, List<Promotion> promociones) {
        LocalDateTime desde = LocalDate.now(reloj).withDayOfYear(1).atStartOfDay();
        LocalDateTime hasta = desde.plusYears(1);
        return promociones.stream()
                .filter(p -> p.getMaxUsesPerUser() != null)
                .collect(Collectors.toMap(Promotion::getIdPromotion,
                        p -> saleTransactionRepository.contarUsos(cliente.getIdUser(), p.getIdPromotion(), desde, hasta)));
    }
}
