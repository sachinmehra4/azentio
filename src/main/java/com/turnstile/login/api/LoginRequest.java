package com.turnstile.login.api;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record LoginRequest(
    @NotBlank @Size(max = 100) String eventId,
    @NotNull Long customerId,
    @NotBlank @Size(max = 160) String deviceId,
    @NotBlank @Size(min = 2, max = 3) String countryCode,
    @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
    @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
    boolean credentialValid,
    @NotNull Instant eventTime
) {}
