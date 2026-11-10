package com.cinemax.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra el control de acceso entre servicios.
 *
 * Los servicios que escanean {@code com.cinemax} lo cargan solos. Un servicio
 * que no lo haga (auth-service) debe importarlo con
 * {@code @Import(ServiceAccessConfig.class)}.
 */
@Configuration
public class ServiceAccessConfig {

    /** Antes que Spring Security (-100): se rechaza sin llegar a validar el JWT. */
    private static final int FILTER_ORDER = -200;

    @Bean
    public FilterRegistrationBean<ServiceAccessFilter> serviceAccessFilter(
            @Value("${cinemax.security.gateway-secret:}") String gatewaySecret,
            @Value("${cinemax.security.internal-token:}") String internalToken) {
        FilterRegistrationBean<ServiceAccessFilter> registration =
                new FilterRegistrationBean<>(new ServiceAccessFilter(gatewaySecret, internalToken));
        registration.setOrder(FILTER_ORDER);
        return registration;
    }

    /** Solo en los servicios que usan Feign. */
    @Configuration
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    static class FeignClientConfig {

        @Bean
        public InternalTokenFeignInterceptor internalTokenFeignInterceptor(
                @Value("${cinemax.security.internal-token:}") String internalToken) {
            return new InternalTokenFeignInterceptor(internalToken);
        }
    }
}
