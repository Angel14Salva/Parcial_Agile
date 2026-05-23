package com.municipalidad.licencias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LicenciasApplication {
    public static void main(String[] args) {
        SpringApplication.run(LicenciasApplication.class, args);
    }
}
