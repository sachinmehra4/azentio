package com.turnstile.data;

import com.turnstile.login.api.LoginRequest;
import com.turnstile.login.repository.LoginRiskRepository;
import com.turnstile.login.service.LoginRiskService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;

/*
 * Run with --turnstile.generator.enabled=true to create 500 customers and 20,000
 * login events. It uses the real service so generated rows exercise scoring.
 */
@Component
@ConditionalOnProperty(name = "turnstile.generator.enabled", havingValue = "true")
public class DemoDataGenerator implements ApplicationRunner {
    private final LoginRiskRepository repository;
    private final LoginRiskService service;

    public DemoDataGenerator(LoginRiskRepository repository, LoginRiskService service) {
        this.repository = repository;
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        Random random = new Random(73);
        for (long customer = 1; customer <= 500; customer++) {
            if (!repository.customerExists(customer)) {
                repository.createCustomer(customer, "Demo Customer " + customer);
            }
        }

        Instant base = Instant.parse("2026-10-01T00:00:00Z");
        for (int i = 0; i < 20_000; i++) {
            long customerId = 1 + random.nextInt(500);
            boolean atoPattern = i % 97 == 0;
            String country = atoPattern ? "GB" : "IN";
            String device = atoPattern ? "new-device-" + i : "device-" + customerId;
            boolean valid = i % 13 != 0;
            Instant at = base.plusSeconds(i * 90L);
            LoginRequest event = new LoginRequest(
                    "seed-event-" + i, customerId, device, country,
                    atoPattern ? 51.5074 : 19.0760,
                    atoPattern ? -0.1278 : 72.8777,
                    valid, at);
            service.evaluate(event);
        }
    }
}
