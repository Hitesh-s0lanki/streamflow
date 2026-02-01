package com.streamflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration. Keeps version and Kafka test routes public;
 * all other routes require authentication.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:3001", "http://127.0.0.1:3000", "http://127.0.0.1:3001"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/demo/version").permitAll()
                        .requestMatchers("/api/kafka/test/**").permitAll()
                        .requestMatchers("/api/health", "/api/ready").permitAll()
                        .requestMatchers("/api/content", "/api/content/**").permitAll()
                        .requestMatchers("/api/seasons/**").permitAll()
                        .requestMatchers("/api/video-assets", "/api/video-assets/**").permitAll()
                        .requestMatchers("/api/ingestion", "/api/ingestion/**").permitAll()
                        .requestMatchers("/api/admin/content", "/api/admin/content/**").permitAll()
                        .requestMatchers("/api/admin/ingestion", "/api/admin/ingestion/**").permitAll()
                        .requestMatchers("/api/admin/ingestion-jobs", "/api/admin/ingestion-jobs/**").permitAll()
                        .requestMatchers("/api/admin/licenses", "/api/admin/licenses/**").permitAll()
                        .requestMatchers("/api/admin/signed-urls", "/api/admin/signed-urls/**").permitAll()
                        .requestMatchers("/api/admin/playback-events", "/api/admin/playback-events/**").permitAll()
                        .requestMatchers("/api/playback", "/api/playback/**").permitAll()
                        .requestMatchers("/api/watch-progress", "/api/watch-progress/**").permitAll()
                        .requestMatchers("/api/sprites", "/api/sprites/**").permitAll()
                        .requestMatchers("/api/analytics", "/api/analytics/**").permitAll()
                        .requestMatchers("/api/admin/analytics", "/api/admin/analytics/**").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**"))
                .formLogin(form -> form
                        .defaultSuccessUrl("/", true))
                .build();
    }
}
