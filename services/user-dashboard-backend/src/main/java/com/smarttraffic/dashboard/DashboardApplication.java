package com.smarttraffic.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Backend del dashboard: recibe el estado de las zonas por RabbitMQ, lo expone
 * por REST y WebSocket, y consulta el historial guardado en PostgreSQL.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardApplication.class, args);
    }
}
