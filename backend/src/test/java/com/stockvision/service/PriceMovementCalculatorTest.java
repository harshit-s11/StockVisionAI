package com.stockvision.service;

import com.stockvision.config.MarketSimulationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PriceMovementCalculatorTest {

    private MarketSimulationProperties properties;
    private PriceMovementCalculator calculator;

    @BeforeEach
    void setUp() {
        properties = new MarketSimulationProperties();
        properties.setEnabled(true);
        properties.setMaxChangePercent(new BigDecimal("1.50"));
        properties.setMinPrice(new BigDecimal("1.00"));
        calculator = new PriceMovementCalculator(properties);
    }

    @Test
    @DisplayName("Calculates positive price movement within configured bounds")
    void testPositiveMovementWithinBounds() {
        // Initial price = 1000.00, factor = +0.50 -> +0.75% -> 1007.50
        BigDecimal currentPrice = new BigDecimal("1000.00");
        BigDecimal nextPrice = calculator.calculateNextPrice(currentPrice, 0.50);

        assertThat(nextPrice).isEqualByComparingTo("1007.50");
    }

    @Test
    @DisplayName("Calculates negative price movement within configured bounds")
    void testNegativeMovementWithinBounds() {
        // Initial price = 1000.00, factor = -0.50 -> -0.75% -> 992.50
        BigDecimal currentPrice = new BigDecimal("1000.00");
        BigDecimal nextPrice = calculator.calculateNextPrice(currentPrice, -0.50);

        assertThat(nextPrice).isEqualByComparingTo("992.50");
    }

    @Test
    @DisplayName("Enforces maximum configured change percentage at +1.0 and -1.0 factor")
    void testMaxChangePercentEnforced() {
        BigDecimal currentPrice = new BigDecimal("1000.00");

        // +1.0 factor -> +1.50% -> 1015.00
        BigDecimal maxUpPrice = calculator.calculateNextPrice(currentPrice, 1.0);
        assertThat(maxUpPrice).isEqualByComparingTo("1015.00");

        // -1.0 factor -> -1.50% -> 985.00
        BigDecimal maxDownPrice = calculator.calculateNextPrice(currentPrice, -1.0);
        assertThat(maxDownPrice).isEqualByComparingTo("985.00");
    }

    @Test
    @DisplayName("Clamps normalized factors exceeding [-1.0, +1.0] range")
    void testFactorClamping() {
        BigDecimal currentPrice = new BigDecimal("1000.00");

        // Factor +3.5 clamped to +1.0 -> 1015.00
        BigDecimal clampedUp = calculator.calculateNextPrice(currentPrice, 3.5);
        assertThat(clampedUp).isEqualByComparingTo("1015.00");

        // Factor -5.0 clamped to -1.0 -> 985.00
        BigDecimal clampedDown = calculator.calculateNextPrice(currentPrice, -5.0);
        assertThat(clampedDown).isEqualByComparingTo("985.00");
    }

    @Test
    @DisplayName("Enforces minimum price floor to prevent zero or negative equity valuations")
    void testPriceFloorEnforced() {
        // Price = 1.01, factor = -1.0 -> 1.01 * 0.985 = 0.99485 -> should floor at minPrice 1.00
        BigDecimal nearFloorPrice = new BigDecimal("1.01");
        BigDecimal nextPrice = calculator.calculateNextPrice(nearFloorPrice, -1.0);

        assertThat(nextPrice).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("Handles zero or null current price gracefully by returning minimum price floor")
    void testZeroOrNullCurrentPrice() {
        assertThat(calculator.calculateNextPrice(BigDecimal.ZERO, 0.5)).isEqualByComparingTo("1.00");
        assertThat(calculator.calculateNextPrice(null, 0.5)).isEqualByComparingTo("1.00");
        assertThat(calculator.calculateNextPrice(new BigDecimal("-50.00"), 0.5)).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("Performs HALF_UP rounding to exact 2 decimal places")
    void testHalfUpRounding() {
        // 3850.00 * (1 + 0.0123) = 3897.355 -> rounded to 3897.36
        properties.setMaxChangePercent(new BigDecimal("1.23"));
        BigDecimal price = new BigDecimal("3850.00");
        BigDecimal nextPrice = calculator.calculateNextPrice(price, 1.0);

        assertThat(nextPrice).isEqualByComparingTo("3897.36");
        assertThat(nextPrice.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Random simulation produces valid prices strictly above floor over multiple iterations")
    void testRandomSimulationStability() {
        BigDecimal price = new BigDecimal("100.00");
        for (int i = 0; i < 100; i++) {
            price = calculator.calculateNextPrice(price);
            assertThat(price).isGreaterThanOrEqualTo(new BigDecimal("1.00"));
            assertThat(price.scale()).isEqualTo(2);
        }
    }
}