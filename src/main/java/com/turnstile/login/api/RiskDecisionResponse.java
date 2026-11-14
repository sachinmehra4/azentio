package com.turnstile.login.api;

import java.time.Instant;
import java.util.List;

public record RiskDecisionResponse(
    Long id,
    String eventId,
    Long customerId,
    int score,
    String decision,
    List<String> reasonCodes,
    String stepUpStatus,
    Instant createdAt
) {}
