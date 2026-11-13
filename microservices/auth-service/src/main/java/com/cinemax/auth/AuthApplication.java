package com.cinemax.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

import com.cinemax.common.error.ApiExceptionHandler;
import com.cinemax.common.error.FeignExceptionHandler;
import com.cinemax.common.error.RespuestasDeSeguridad;
import com.cinemax.common.security.ServiceAccessConfig;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
// Este servicio no escanea com.cinemax.common: importa el control de acceso y los
// errores con el formato común.
@Import({ServiceAccessConfig.class, ApiExceptionHandler.class, FeignExceptionHandler.class, RespuestasDeSeguridad.class})
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}