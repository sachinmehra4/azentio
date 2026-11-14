package com.turnstile.login.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LoginScenarioCatalogueTest {
    static Stream<JsonNode> scenarios() throws Exception {
        try (InputStream in = LoginScenarioCatalogueTest.class
                .getResourceAsStream("/scenarios/login-risk-scenarios.json")) {
            assertNotNull(in);
            JsonNode root = new ObjectMapper().readTree(in);
            ArrayList<JsonNode> list = new ArrayList<>();
            root.forEach(list::add);
            return list.stream();
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenarios")
    void everyScenarioIsNamedAndDefinesExpectedOutput(JsonNode scenario) {
        assertTrue(scenario.hasNonNull("name"));
        assertTrue(scenario.hasNonNull("expectedDecision"));
        assertTrue(scenario.hasNonNull("expectedScore"));
        assertTrue(scenario.hasNonNull("expectedReasons"));
    }
}
