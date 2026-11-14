package com.turnstile.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "turnstile.scoring")
public class ScoringProperties {
    private int newDevicePoints;
    private int newCountryPoints;
    private int impossibleTravelPoints;
    private int failedBurstPoints;
    private int allowMaxExclusive;
    private int stepUpMaxExclusive;
    private int failedBurstCount;
    private int failedWindowMinutes;
    private int countryLookbackDays;
    private double impossibleTravelSpeedKmh;
    private double earthRadiusKm;

    public int getNewDevicePoints() { return newDevicePoints; }
    public void setNewDevicePoints(int v) { newDevicePoints = v; }
    public int getNewCountryPoints() { return newCountryPoints; }
    public void setNewCountryPoints(int v) { newCountryPoints = v; }
    public int getImpossibleTravelPoints() { return impossibleTravelPoints; }
    public void setImpossibleTravelPoints(int v) { impossibleTravelPoints = v; }
    public int getFailedBurstPoints() { return failedBurstPoints; }
    public void setFailedBurstPoints(int v) { failedBurstPoints = v; }
    public int getAllowMaxExclusive() { return allowMaxExclusive; }
    public void setAllowMaxExclusive(int v) { allowMaxExclusive = v; }
    public int getStepUpMaxExclusive() { return stepUpMaxExclusive; }
    public void setStepUpMaxExclusive(int v) { stepUpMaxExclusive = v; }
    public int getFailedBurstCount() { return failedBurstCount; }
    public void setFailedBurstCount(int v) { failedBurstCount = v; }
    public int getFailedWindowMinutes() { return failedWindowMinutes; }
    public void setFailedWindowMinutes(int v) { failedWindowMinutes = v; }
    public int getCountryLookbackDays() { return countryLookbackDays; }
    public void setCountryLookbackDays(int v) { countryLookbackDays = v; }
    public double getImpossibleTravelSpeedKmh() { return impossibleTravelSpeedKmh; }
    public void setImpossibleTravelSpeedKmh(double v) { impossibleTravelSpeedKmh = v; }
    public double getEarthRadiusKm() { return earthRadiusKm; }
    public void setEarthRadiusKm(double v) { earthRadiusKm = v; }
}
