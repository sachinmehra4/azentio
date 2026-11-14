package com.turnstile.login.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeoDistanceTest {
    private final GeoDistance geo = new GeoDistance();

    @Test
    void samePointIsZero() {
        assertEquals(0.0, geo.haversineKm(12.97, 77.59, 12.97, 77.59, 6371.0088), 0.0001);
    }

    @Test
    void oneDegreeAtEquatorIsAbout111Km() {
        assertEquals(111.2, geo.haversineKm(0, 0, 0, 1, 6371.0088), 1.0);
    }
}
