package com.smarttraffic.archiver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto de entrada del archivador: consume {@code raw_traffic_data} de Kafka y
 * guarda cada lectura en la tabla {@code traffic_readings_history} de PostgreSQL.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class TrafficArchiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrafficArchiverApplication.class, args);
    }
}
