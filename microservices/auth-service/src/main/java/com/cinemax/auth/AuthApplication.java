package com.cinemax.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

import com.cinemax.common.security.ServiceAccessConfig;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
// Este servicio no escanea com.cinemax.common: importa solo el control de acceso.
@Import(ServiceAccessConfig.class)
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}