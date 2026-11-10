package com.cinemax.common.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceAccessFilterTest {

    private static final String GATEWAY_SECRET = "secreto-del-gateway";
    private static final String INTERNAL_TOKEN = "token-de-servicio";

    private final ServiceAccessFilter filter = new ServiceAccessFilter(GATEWAY_SECRET, INTERNAL_TOKEN);

    @Test
    void apiAceptaElSecretoDelGateway() throws Exception {
        MockHttpServletRequest request = request("/api/movies");
        request.addHeader(ServiceAccessFilter.GATEWAY_SECRET_HEADER, GATEWAY_SECRET);

        assertThat(statusOf(request)).isEqualTo(200);
    }

    @Test
    void apiAceptaElTokenDeServicio() throws Exception {
        MockHttpServletRequest request = request("/api/users/profile");
        request.addHeader(ServiceAccessFilter.INTERNAL_TOKEN_HEADER, INTERNAL_TOKEN);

        assertThat(statusOf(request)).isEqualTo(200);
    }

    @Test
    void apiRechazaUnaPeticionDirecta() throws Exception {
        assertThat(statusOf(request("/api/movies"))).isEqualTo(403);
    }

    @Test
    void apiRechazaLaCabeceraFijaAntigua() throws Exception {
        MockHttpServletRequest request = request("/api/movies");
        request.addHeader("X-Gateway-Request", "true");

        assertThat(statusOf(request)).isEqualTo(403);
    }

    @Test
    void apiRechazaUnSecretoIncorrecto() throws Exception {
        MockHttpServletRequest request = request("/api/movies");
        request.addHeader(ServiceAccessFilter.GATEWAY_SECRET_HEADER, GATEWAY_SECRET + "x");

        assertThat(statusOf(request)).isEqualTo(403);
    }

    @Test
    void internalAceptaElTokenDeServicio() throws Exception {
        MockHttpServletRequest request = request("/internal/seats/1");
        request.addHeader(ServiceAccessFilter.INTERNAL_TOKEN_HEADER, INTERNAL_TOKEN);

        assertThat(statusOf(request)).isEqualTo(200);
    }

    @Test
    void internalRechazaUnaPeticionSinToken() throws Exception {
        assertThat(statusOf(request("/internal/seats/1"))).isEqualTo(403);
    }

    @Test
    void internalRechazaUnTokenIncorrecto() throws Exception {
        MockHttpServletRequest request = request("/internal/seats/1");
        request.addHeader(ServiceAccessFilter.INTERNAL_TOKEN_HEADER, "otro-token");

        assertThat(statusOf(request)).isEqualTo(403);
    }

    @Test
    void internalNoAceptaElSecretoDelGateway() throws Exception {
        MockHttpServletRequest request = request("/internal/seats/1");
        request.addHeader(ServiceAccessFilter.GATEWAY_SECRET_HEADER, GATEWAY_SECRET);

        assertThat(statusOf(request)).isEqualTo(403);
    }

    @Test
    void decideConLaRutaDecodificadaYNoConElTextoCrudo() throws Exception {
        // /%69nternal/seats/1 llega al controlador de /internal/seats/1.
        MockHttpServletRequest request = request("/internal/seats/1");
        request.setRequestURI("/%69nternal/seats/1");
        request.addHeader(ServiceAccessFilter.GATEWAY_SECRET_HEADER, GATEWAY_SECRET);

        assertThat(statusOf(request)).isEqualTo(403);
    }

    @Test
    void unaRutaDesconocidaTambienExigeCredenciales() throws Exception {
        assertThat(statusOf(request("/v3/api-docs"))).isEqualTo(403);
    }

    @Test
    void actuatorQuedaAbiertoParaLosHealthchecks() throws Exception {
        assertThat(statusOf(request("/actuator/health"))).isEqualTo(200);
    }

    @Test
    void laReglaDeSecurityConfigSoloPermiteLlamadasConToken() throws Exception {
        MockHttpServletRequest withToken = request("/internal/seats/1");
        withToken.addHeader(ServiceAccessFilter.INTERNAL_TOKEN_HEADER, INTERNAL_TOKEN);
        filter.doFilter(withToken, new MockHttpServletResponse(), new MockFilterChain());

        // Sin pasar por el filtro: la regla deniega.
        MockHttpServletRequest unfiltered = request("/internal/seats/1");

        assertThat(decisionFor(withToken).isGranted()).isTrue();
        assertThat(decisionFor(unfiltered).isGranted()).isFalse();
    }

    @Test
    void noArrancaSinSecretos() {
        assertThatThrownBy(() -> new ServiceAccessFilter("", INTERNAL_TOKEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GATEWAY_SECRET");
        assertThatThrownBy(() -> new ServiceAccessFilter(GATEWAY_SECRET, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INTERNAL_TOKEN");
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }

    /** 200 si el filtro deja pasar la petición; si no, el estado con que la rechaza. */
    private int statusOf(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return response.getStatus();
    }

    private static AuthorizationDecision decisionFor(MockHttpServletRequest request) {
        return ServiceAccessFilter.internalCallsOnly()
                .check(() -> null, new RequestAuthorizationContext(request));
    }
}
