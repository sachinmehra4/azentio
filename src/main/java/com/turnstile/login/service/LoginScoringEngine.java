package com.turnstile.login.service;

import com.turnstile.config.ScoringProperties;
import com.turnstile.login.model.ReasonCode;
import com.turnstile.login.model.RiskBand;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class LoginScoringEngine {
    private final ScoringProperties properties;

    public LoginScoringEngine(ScoringProperties properties) {
        this.properties = properties;
    }

    public ScoringResult score(boolean newDevice, boolean newCountry,
                               boolean impossibleTravel, boolean failedBurst) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (newDevice) {
            score += properties.getNewDevicePoints();
            reasons.add(ReasonCode.NEW_DEVICE.name());
        }
        if (newCountry) {
            score += properties.getNewCountryPoints();
            reasons.add(ReasonCode.NEW_COUNTRY.name());
        }
        if (impossibleTravel) {
            score += properties.getImpossibleTravelPoints();
            reasons.add(ReasonCode.IMPOSSIBLE_TRAVEL.name());
        }
        if (failedBurst) {
            score += properties.getFailedBurstPoints();
            reasons.add(ReasonCode.FAILED_BURST.name());
        }

        RiskBand band = score < properties.getAllowMaxExclusive()
                ? RiskBand.ALLOW
                : score < properties.getStepUpMaxExclusive()
                    ? RiskBand.STEP_UP : RiskBand.DENY;

        return new ScoringResult(score, band.name(), List.copyOf(reasons));
    }
}
