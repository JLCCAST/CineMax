package com.cinemax.usuariosservice.config;

import com.cinemax.common.error.RespuestasDeSeguridad;
import com.cinemax.common.security.JwtAuthenticationFilter;
import com.cinemax.common.security.ServiceAccessFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RespuestasDeSeguridad respuestas;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // Sin sesión: 401; con sesión pero sin permiso: 403. Ambos con el formato común.
            .exceptionHandling(errores -> errores.authenticationEntryPoint(respuestas).accessDeniedHandler(respuestas))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Servicio-a-servicio (auth-service vía Feign): exige el token de servicio.
                .requestMatchers("/internal/**").access(ServiceAccessFilter.internalCallsOnly())

                // Flujo público de auth (forgot/reset-password)
                .requestMatchers("/api/auth/**").permitAll()

                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                .requestMatchers("/api/roles").permitAll()

                .requestMatchers("/api/documents").permitAll()

                .requestMatchers("/actuator/health").permitAll()

                .anyRequest().authenticated()
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}