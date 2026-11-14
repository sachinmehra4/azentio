package com.turnstile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TurnstileApplication {
    public static void main(String[] args) {
        SpringApplication.run(TurnstileApplication.class, args);
    }
}
