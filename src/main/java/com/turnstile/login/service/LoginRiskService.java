package com.turnstile.login.service;

import com.turnstile.config.ScoringProperties;
import com.turnstile.login.api.LoginRequest;
import com.turnstile.login.api.RiskDecisionResponse;
import com.turnstile.login.model.LoginOutcome;
import com.turnstile.login.model.ReasonCode;
import com.turnstile.login.repository.LoginRiskRepository;
import com.turnstile.login.repository.LoginRiskRepository.PreviousLogin;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class LoginRiskService {
    private final LoginRiskRepository repository;
    private final ScoringProperties properties;
    private final GeoDistance geoDistance;
    private final LoginScoringEngine scoringEngine;
    private final Clock clock;

    public LoginRiskService(LoginRiskRepository repository, ScoringProperties properties,
                            GeoDistance geoDistance, LoginScoringEngine scoringEngine,
                            Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.geoDistance = geoDistance;
        this.scoringEngine = scoringEngine;
        this.clock = clock;
    }

    /*
     * Synchronized in-process to serialize identical requests in this single application
     * instance. The database UNIQUE(event_id) constraint is the final cross-instance guard.
     * For multi-node production deployment, use a database advisory lock or an idempotency
     * reservation table with retry-on-unique-violation.
     */
    @Transactional
    public synchronized RiskDecisionResponse evaluate(LoginRequest request) {
        var existing = repository.findDecision(request.eventId());
        if (existing.isPresent()) return existing.get();

        if (!repository.customerExists(request.customerId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND");
        }

        Instant eventTime = request.eventTime();

        if (!request.credentialValid()) {
            repository.saveAttempt(request, LoginOutcome.FAILED.name());
            return repository.saveDecision(request.eventId(), request.customerId(), 0,
                    "DENY", List.of(ReasonCode.BAD_CREDENTIALS.name()),
                    "NOT_REQUIRED", clock.instant());
        }

        boolean newDevice = !repository.isTrusted(request.customerId(), request.deviceId());

        Instant countryFrom = eventTime.minus(Duration.ofDays(properties.getCountryLookbackDays()));
        boolean newCountry = !repository.hasPassedCountry(
                request.customerId(), request.countryCode().toUpperCase(), countryFrom, eventTime);

        PreviousLogin previous = repository.findPreviousPassed(request.customerId(), eventTime);
        boolean impossibleTravel = false;

        if (previous != null) {
            double distance = geoDistance.haversineKm(
                    previous.latitude(), previous.longitude(),
                    request.latitude(), request.longitude(),
                    properties.getEarthRadiusKm());

            double elapsedHours = Duration.between(previous.eventTime(), eventTime)
                    .toMillis() / 3_600_000.0;

            if (elapsedHours > 0) {
                impossibleTravel = distance / elapsedHours
                        > properties.getImpossibleTravelSpeedKmh();
            }
        }

        Instant failedWindowStart = eventTime.minus(
                Duration.ofMinutes(properties.getFailedWindowMinutes()));
        int failedCount = repository.countFailedInWindow(
                request.customerId(), failedWindowStart, eventTime);
        boolean failedBurst = failedCount >= properties.getFailedBurstCount();

        ScoringResult result = scoringEngine.score(
                newDevice, newCountry, impossibleTravel, failedBurst);

        // A valid-credential attempt is recorded as PASSED for signal history,
        // even if the risk engine returns STEP_UP or DENY.
        repository.saveAttempt(request, LoginOutcome.PASSED.name());

        if ("ALLOW".equals(result.decision())) {
            repository.trustDevice(request.customerId(), request.deviceId(), clock.instant());
        }

        String stepUpStatus = "STEP_UP".equals(result.decision())
                ? "PENDING" : "NOT_REQUIRED";

        return repository.saveDecision(request.eventId(), request.customerId(),
                result.score(), result.decision(), result.reasonCodes(),
                stepUpStatus, clock.instant());
    }

    @Transactional
    public synchronized RiskDecisionResponse recordStepUp(long decisionId, boolean passed) {
        RiskDecisionResponse decision = repository.findDecisionById(decisionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DECISION_NOT_FOUND"));

        if (!"STEP_UP".equals(decision.decision())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "STEP_UP_NOT_REQUIRED");
        }

        if (!"PENDING".equals(decision.stepUpStatus())) {
            return decision;
        }

        repository.updateStepUp(decisionId, passed ? "PASSED" : "FAILED");

        if (passed) {
            repository.trustDeviceForEvent(decision.eventId(), clock.instant());
        }

        return repository.findDecisionById(decisionId).orElseThrow();
    }
}
