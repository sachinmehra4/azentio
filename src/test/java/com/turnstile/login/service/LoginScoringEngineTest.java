package com.turnstile.login.service;

import com.turnstile.config.ScoringProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class LoginScoringEngineTest {
    private ScoringProperties config() {
        ScoringProperties p = new ScoringProperties();
        p.setNewDevicePoints(40);
        p.setNewCountryPoints(25);
        p.setImpossibleTravelPoints(50);
        p.setFailedBurstPoints(30);
        p.setAllowMaxExclusive(40);
        p.setStepUpMaxExclusive(70);
        return p;
    }

    @Test
    void thresholdChangeComesOnlyFromConfiguration() {
        ScoringProperties p = config();
        LoginScoringEngine engine = new LoginScoringEngine(p);
        assertEquals("STEP_UP", engine.score(true, false, false, false).decision());

        p.setAllowMaxExclusive(45);
        assertEquals("ALLOW", engine.score(true, false, false, false).decision());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "no signals,false,false,false,false,0,ALLOW",
        "new device,true,false,false,false,40,STEP_UP",
        "new country,false,true,false,false,25,ALLOW",
        "impossible travel,false,false,true,false,50,STEP_UP",
        "failed burst,false,false,false,true,30,ALLOW",
        "new device and country,true,true,false,false,65,STEP_UP",
        "new device and travel,true,false,true,false,90,DENY",
        "new country and failed burst,false,true,false,true,55,STEP_UP",
        "all signals,true,true,true,true,145,DENY"
    })
    void scoresSignalCombinations(String name, boolean device, boolean country,
                                  boolean travel, boolean burst, int score, String band) {
        ScoringResult result = new LoginScoringEngine(config()).score(device, country, travel, burst);
        assertEquals(score, result.score());
        assertEquals(band, result.decision());
    }
}
