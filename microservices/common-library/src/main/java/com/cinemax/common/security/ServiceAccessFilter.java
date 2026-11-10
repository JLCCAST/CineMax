package com.cinemax.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Decide si una petición puede entrar al servicio según quién la envía.
 *
 * <ul>
 *   <li>{@code /internal/**}: solo otro servicio, que se identifica con el token de
 *       servicio en la cabecera {@value #INTERNAL_TOKEN_HEADER}.</li>
 *   <li>{@code /actuator/**}: abierto a este filtro; lo protege Spring Security.</li>
 *   <li>Todo lo demás (la API pública bajo {@code /api/**}): el gateway, que se
 *       identifica con su secreto en la cabecera {@value #GATEWAY_SECRET_HEADER},
 *       u otro servicio con el token de servicio.</li>
 * </ul>
 *
 * Reemplaza a las copias de {@code GatewayRequestFilter} que había en cada
 * servicio, que aceptaban el valor fijo {@code X-Gateway-Request: true}.
 */
public class ServiceAccessFilter extends OncePerRequestFilter {

    public static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private static final String INTERNAL_CALL_ATTRIBUTE =
            ServiceAccessFilter.class.getName() + ".internalCall";

    private final byte[] gatewaySecret;
    private final byte[] internalToken;

    public ServiceAccessFilter(String gatewaySecret, String internalToken) {
        this.gatewaySecret = required(gatewaySecret, "GATEWAY_SECRET");
        this.internalToken = required(internalToken, "INTERNAL_TOKEN");
    }

    /**
     * Regla para {@code SecurityConfig}: permite la petición solo si este filtro
     * comprobó el token de servicio. Si el filtro no estuviera registrado, la
     * regla deniega en lugar de dejar {@code /internal/**} abierto.
     */
    public static AuthorizationManager<RequestAuthorizationContext> internalCallsOnly() {
        return (authentication, context) -> new AuthorizationDecision(
                Boolean.TRUE.equals(context.getRequest().getAttribute(INTERNAL_CALL_ATTRIBUTE)));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = pathOf(request);
        boolean fromService = matches(request.getHeader(INTERNAL_TOKEN_HEADER), internalToken);
        if (fromService) {
            request.setAttribute(INTERNAL_CALL_ATTRIBUTE, Boolean.TRUE);
        }

        boolean allowed;
        if (path.startsWith("/internal/")) {
            allowed = fromService;
        } else if (path.startsWith("/actuator/")) {
            allowed = true;
        } else {
            allowed = fromService
                    || matches(request.getHeader(GATEWAY_SECRET_HEADER), gatewaySecret);
        }

        if (!allowed) {
            reject(response);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * Ruta ya decodificada y normalizada por el contenedor. No se usa
     * {@code getRequestURI()} porque es el texto crudo: {@code /%69nternal/x}
     * no empezaría por {@code /internal/} y aun así llegaría a ese controlador.
     */
    private static String pathOf(HttpServletRequest request) {
        String path = request.getServletPath();
        if (request.getPathInfo() != null) {
            path += request.getPathInfo();
        }
        return path;
    }

    private static boolean matches(String received, byte[] expected) {
        return received != null
                && MessageDigest.isEqual(received.getBytes(StandardCharsets.UTF_8), expected);
    }

    private static void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"status\":403,\"error\":\"Forbidden\","
                        + "\"message\":\"Acceso denegado: la petición no viene del gateway ni de otro servicio.\"}");
    }

    private static byte[] required(String value, String variable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Falta " + variable + ". Defínela en el entorno del servicio "
                            + "(en local, en microservices/.env).");
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
