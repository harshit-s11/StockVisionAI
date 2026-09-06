package com.stockvision.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "market.simulation")
public class MarketSimulationProperties {

    /**
     * Controls whether the background simulated market price ticker is enabled.
     */
    private boolean enabled = true;

    /**
     * Delay between simulation ticks in milliseconds (default: 15 seconds).
     */
    private long intervalMs = 15000L;

    /**
     * Initial startup delay before the first simulation tick in milliseconds (default: 10 seconds).
     */
    private long initialDelayMs = 10000L;

    /**
     * Maximum percentage price fluctuation per simulation tick (e.g. 1.50%).
     */
    private BigDecimal maxChangePercent = new BigDecimal("1.50");

    /**
     * Absolute price floor to prevent simulated stock prices from reaching zero or negative numbers.
     */
    private BigDecimal minPrice = new BigDecimal("1.00");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    public BigDecimal getMaxChangePercent() {
        return maxChangePercent;
    }

    public void setMaxChangePercent(BigDecimal maxChangePercent) {
        this.maxChangePercent = maxChangePercent;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }
}