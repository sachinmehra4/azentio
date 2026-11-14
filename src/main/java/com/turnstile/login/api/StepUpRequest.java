package com.turnstile.login.api;

import jakarta.validation.constraints.NotNull;

public record StepUpRequest(@NotNull Boolean passed) {}
