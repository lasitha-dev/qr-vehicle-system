package com.uop.qrvehicle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * QR Vehicle Management System
 * University of Peradeniya - Information Technology Center
 * 
 * Main Spring Boot Application Entry Point
 */
@SpringBootApplication
@EnableAsync
public class QrVehicleApplication {

    public static void main(String[] args) {
        SpringApplication.run(QrVehicleApplication.class, args);
    }
}
