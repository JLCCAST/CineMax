package com.cinemax.cartelera.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Los horarios de las funciones están en hora de Lima y el contenedor corre en UTC:
// toda comparación con "ahora" usa este reloj.
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Lima"));
    }
}
