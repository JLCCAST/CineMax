package com.cinemax.auth.config;

import com.cinemax.common.error.RespuestasDeSeguridad;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, RespuestasDeSeguridad respuestas) throws Exception {
        http           
                .csrf(AbstractHttpConfigurer::disable)
                // Sin sesión: 401; con sesión pero sin permiso: 403. Ambos con el formato común.
                .exceptionHandling(errores -> errores.authenticationEntryPoint(respuestas).accessDeniedHandler(respuestas))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

}