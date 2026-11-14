package com.turnstile.login.service;

import com.turnstile.config.ScoringProperties;
import com.turnstile.login.api.LoginRequest;
import com.turnstile.login.api.RiskDecisionResponse;
import com.turnstile.login.repository.LoginRiskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LoginRiskServiceTest {
    @Autowired LoginRiskService service;
    @Autowired LoginRiskRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired ScoringProperties properties;

    private static final Instant T = Instant.parse("2026-11-14T06:00:00Z");

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM risk_decision");
        jdbc.update("DELETE FROM login_attempt");
        jdbc.update("DELETE FROM trusted_device");
        jdbc.update("DELETE FROM customer");
        repository.createCustomer(1001L, "Test Customer");
    }

    @Test
    void firstPassedLoginSkipsImpossibleTravel() {
        RiskDecisionResponse response = service.evaluate(req("first", "device-1", "IN", 12.97, 77.59, true, T));
        assertFalse(response.reasonCodes().contains("IMPOSSIBLE_TRAVEL"));
        assertTrue(response.reasonCodes().contains("NEW_DEVICE"));
    }

    @Test
    void idempotentReplayReturnsOriginalDecisionAndDoesNotAddAttempt() {
        LoginRequest request = req("same-event", "device-2", "IN", 12.97, 77.59, true, T);
        RiskDecisionResponse first = service.evaluate(request);
        int attemptsAfterFirst = repository.countAttempts();

        RiskDecisionResponse replay = service.evaluate(request);

        assertEquals(first.id(), replay.id());
        assertEquals(first.score(), replay.score());
        assertEquals(attemptsAfterFirst, repository.countAttempts());
    }

    @Test
    void stepUpPassedTrustsDevice() {
        RiskDecisionResponse response = service.evaluate(
                req("step-up-event", "untrusted", "IN", 12.97, 77.59, true, T));
        assertEquals("STEP_UP", response.decision());

        RiskDecisionResponse updated = service.recordStepUp(response.id(), true);
        assertEquals("PASSED", updated.stepUpStatus());
        assertTrue(repository.isTrusted(1001L, "untrusted"));
    }

    @Test
    void failedWindowIncludesLowerBoundaryAndExcludesUpperBoundary() {
        // Five failed attempts at exactly t-15m count (inclusive lower bound).
        for (int i = 0; i < 5; i++) {
            LoginRequest failed = req("fail-lower-" + i, "bad-device", "IN",
                    12.97, 77.59, false, T.minusSeconds(900));
            service.evaluate(failed);
        }
        RiskDecisionResponse included = service.evaluate(
                req("window-check", "new-device", "IN", 12.97, 77.59, true, T));
        assertTrue(included.reasonCodes().contains("FAILED_BURST"));

        // The event timestamp is exclusive. Five failed attempts exactly at t
        // are not prior attempts for a separate evaluation at t.
        for (int i = 0; i < 5; i++) {
            service.evaluate(req("upper-" + i, "bad-device", "IN",
                    12.97, 77.59, false, T.plusSeconds(60)));
        }
        RiskDecisionResponse upper = service.evaluate(
                req("upper-check", "trusted-clean", "IN", 12.97, 77.59, true, T.plusSeconds(60)));
        assertFalse(upper.reasonCodes().contains("FAILED_BURST"));
    }

    private LoginRequest req(String id, String device, String country,
                             double lat, double lon, boolean valid, Instant at) {
        return new LoginRequest(id, 1001L, device, country, lat, lon, valid, at);
    }
}
