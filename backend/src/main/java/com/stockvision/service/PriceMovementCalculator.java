package com.stockvision.service;

import com.stockvision.config.MarketSimulationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

@Component
public class PriceMovementCalculator {

    private final MarketSimulationProperties properties;
    private final Random random;

    @Autowired
    public PriceMovementCalculator(MarketSimulationProperties properties) {
        this(properties, new Random());
    }

    public PriceMovementCalculator(MarketSimulationProperties properties, Random random) {
        this.properties = properties;
        this.random = random;
    }

    /**
     * Calculates the next simulated price using a controlled stochastic percentage shift.
     *
     * @param currentPrice The current stock price.
     * @param normalizedFactor Normalized stochastic factor in range [-1.0, +1.0].
     * @return The next simulated stock price rounded to 2 decimal places with HALF_UP.
     */
    public BigDecimal calculateNextPrice(BigDecimal currentPrice, double normalizedFactor) {
        BigDecimal minPrice = properties.getMinPrice() != null
            ? properties.getMinPrice().setScale(2, RoundingMode.HALF_UP)
            : new BigDecimal("1.00");

        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return minPrice;
        }

        // Clamp factor strictly to [-1.0, +1.0]
        double clampedFactor = Math.max(-1.0, Math.min(1.0, normalizedFactor));
        BigDecimal factor = BigDecimal.valueOf(clampedFactor);

        BigDecimal maxPercent = properties.getMaxChangePercent() != null
            ? properties.getMaxChangePercent()
            : new BigDecimal("1.50");

        // percentShift = maxChangePercent * factor
        BigDecimal percentShift = maxPercent.multiply(factor).setScale(4, RoundingMode.HALF_UP);

        // multiplier = 1 + (percentShift / 100)
        BigDecimal multiplier = BigDecimal.ONE.add(
            percentShift.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
        );

        BigDecimal nextPrice = currentPrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        if (nextPrice.compareTo(minPrice) < 0) {
            return minPrice;
        }

        return nextPrice;
    }

    /**
     * Calculates the next simulated price using the configured random generator.
     */
    public BigDecimal calculateNextPrice(BigDecimal currentPrice) {
        // Generate uniform random in range [-1.0, +1.0]
        double randomFactor = (random.nextDouble() * 2.0) - 1.0;
        return calculateNextPrice(currentPrice, randomFactor);
    }
}