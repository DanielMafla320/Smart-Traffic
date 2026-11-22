package com.smarttraffic.dashboard.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS: permite que el dashboard.html, servido desde otro origen (otro puerto
 * o Nginx), llame a la API con fetch(). Solo se permiten los orígenes
 * configurados y solo métodos de lectura.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DashboardProperties.class)
public class CorsConfig implements WebMvcConfigurer {

    private final DashboardProperties properties;

    public CorsConfig(DashboardProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.allowedOriginsArray())
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
