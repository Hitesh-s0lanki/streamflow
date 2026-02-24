package com.streamflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for the Streamflow API.
 * Allows cross-origin requests from configured frontend origins.
 */
@Configuration
public class CorsConfig {

    @Value("${streamflow.cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private String allowedOrigins;

    @Value("${streamflow.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${streamflow.cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${streamflow.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${streamflow.cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Set allowed origins
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);
        
        // Set allowed methods
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        config.setAllowedMethods(methods);
        
        // Set allowed headers
        if ("*".equals(allowedHeaders)) {
            config.addAllowedHeader("*");
        } else {
            config.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        }
        
        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(allowCredentials);
        
        // Set max age for preflight cache
        config.setMaxAge(maxAge);
        
        // Expose headers that the client can access
        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition",
                "X-Correlation-Id"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        
        return new CorsFilter(source);
    }
}
