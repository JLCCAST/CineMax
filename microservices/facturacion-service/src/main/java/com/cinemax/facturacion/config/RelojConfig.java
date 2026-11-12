package com.cinemax.facturacion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

// Los horarios de las funciones están en hora de Lima y el contenedor corre en UTC:
// toda comparación con "ahora" usa este reloj.
@Configuration
public class RelojConfig {

    public static final ZoneId ZONA_CINE = ZoneId.of("America/Lima");

    @Bean
    public Clock reloj() {
        return Clock.system(ZONA_CINE);
    }
}
