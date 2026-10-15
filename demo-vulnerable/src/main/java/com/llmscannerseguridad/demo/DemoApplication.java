package com.llmscannerseguridad.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        try {
            SpringApplication.run(DemoApplication.class, args);
        } catch (RuntimeException error) {
            // No imprimir valores de configuración ni causas que contengan fixtures.
            System.err.println("DEMO_STARTUP_FAILED");
            System.exit(1);
        }
    }
}
