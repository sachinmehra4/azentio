package com.turnstile.login.service;

import org.springframework.stereotype.Component;

@Component
public class GeoDistance {
    public double haversineKm(double lat1, double lon1, double lat2, double lon2, double radiusKm) {
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2 - lat1);
        double dl = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2)
                * Math.sin(dl / 2) * Math.sin(dl / 2);

        return 2 * radiusKm * Math.asin(Math.sqrt(Math.min(1.0, a)));
    }
}
