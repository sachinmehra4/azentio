package com.turnstile.login.service;

import java.util.List;

public record ScoringResult(int score, String decision, List<String> reasonCodes) {}
