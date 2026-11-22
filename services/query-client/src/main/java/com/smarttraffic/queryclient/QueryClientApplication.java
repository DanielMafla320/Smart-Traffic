package com.smarttraffic.queryclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QueryClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueryClientApplication.class, args);
    }
}
