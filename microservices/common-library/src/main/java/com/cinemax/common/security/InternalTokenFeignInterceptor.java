package com.cinemax.common.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Agrega el token de servicio a todas las llamadas Feign, para que el servicio
 * de destino acepte la petición (ver {@link ServiceAccessFilter}).
 */
public class InternalTokenFeignInterceptor implements RequestInterceptor {

    private final String internalToken;

    public InternalTokenFeignInterceptor(String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header(ServiceAccessFilter.INTERNAL_TOKEN_HEADER, internalToken);
    }
}
