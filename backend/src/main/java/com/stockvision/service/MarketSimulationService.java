package com.stockvision.service;

import com.stockvision.config.MarketSimulationProperties;
import com.stockvision.entity.StockEntity;
import com.stockvision.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class MarketSimulationService {

    private static final Logger log = LoggerFactory.getLogger(MarketSimulationService.class);

    private final StockRepository stockRepository;
    private final PriceMovementCalculator priceCalculator;
    private final MarketSimulationProperties properties;

    public MarketSimulationService(StockRepository stockRepository,
                                   PriceMovementCalculator priceCalculator,
                                   MarketSimulationProperties properties) {
        this.stockRepository = stockRepository;
        this.priceCalculator = priceCalculator;
        this.properties = properties;
    }

    @Scheduled(
        fixedDelayString = "${market.simulation.interval-ms:15000}",
        initialDelayString = "${market.simulation.initial-delay-ms:10000}"
    )
    @Transactional
    public void simulateMarketTick() {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            List<StockEntity> stocks = stockRepository.findAll();
            if (stocks.isEmpty()) {
                return;
            }

            Instant now = Instant.now();
            for (StockEntity stock : stocks) {
                BigDecimal currentPrice = stock.getCurrentPrice();
                BigDecimal nextPrice = priceCalculator.calculateNextPrice(currentPrice);

                stock.setCurrentPrice(nextPrice);
                stock.setUpdatedAt(now);
            }

            stockRepository.saveAll(stocks);
            log.debug("Simulated market tick updated {} stock quotes.", stocks.size());
        } catch (Exception ex) {
            log.error("Failed to execute scheduled market simulation tick: {}", ex.getMessage(), ex);
        }
    }
}