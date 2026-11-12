package com.cinemax.cartelera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = {"com.cinemax.cartelera", "com.cinemax.common"})
@ComponentScan(basePackages = "com.cinemax")
@EnableDiscoveryClient
@EnableFeignClients
public class CarteleraApplication {
    public static void main(String[] args) {
        SpringApplication.run(CarteleraApplication.class, args);
    }
}