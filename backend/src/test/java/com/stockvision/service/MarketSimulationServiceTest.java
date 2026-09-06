package com.stockvision.service;

import com.stockvision.config.MarketSimulationProperties;
import com.stockvision.entity.StockEntity;
import com.stockvision.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketSimulationServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private PriceMovementCalculator priceCalculator;

    @Captor
    private ArgumentCaptor<List<StockEntity>> stocksCaptor;

    private MarketSimulationProperties properties;
    private MarketSimulationService simulationService;

    @BeforeEach
    void setUp() {
        properties = new MarketSimulationProperties();
        properties.setEnabled(true);
        simulationService = new MarketSimulationService(stockRepository, priceCalculator, properties);
    }

    @Test
    @DisplayName("Updates and persists all active stocks during simulation tick")
    void testSimulateMarketTickUpdatesAllStocks() {
        StockEntity tcs = new StockEntity("TCS", "Tata Consultancy", "IT", new BigDecimal("3850.00"), new BigDecimal("3810.00"));
        StockEntity infy = new StockEntity("INFY", "Infosys", "IT", new BigDecimal("1620.00"), new BigDecimal("1605.00"));

        when(stockRepository.findAll()).thenReturn(List.of(tcs, infy));
        when(priceCalculator.calculateNextPrice(new BigDecimal("3850.00"))).thenReturn(new BigDecimal("3865.00"));
        when(priceCalculator.calculateNextPrice(new BigDecimal("1620.00"))).thenReturn(new BigDecimal("1610.00"));

        simulationService.simulateMarketTick();

        verify(stockRepository, times(1)).saveAll(stocksCaptor.capture());
        List<StockEntity> savedStocks = stocksCaptor.getValue();

        assertThat(savedStocks).hasSize(2);
        assertThat(savedStocks.get(0).getCurrentPrice()).isEqualByComparingTo("3865.00");
        assertThat(savedStocks.get(1).getCurrentPrice()).isEqualByComparingTo("1610.00");
    }

    @Test
    @DisplayName("Does nothing when simulation is disabled in configuration")
    void testDisabledSimulationDoesNothing() {
        properties.setEnabled(false);

        simulationService.simulateMarketTick();

        verify(stockRepository, never()).findAll();
        verify(stockRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Handles empty stock catalog gracefully without errors")
    void testEmptyStockCatalogHandledSafely() {
        when(stockRepository.findAll()).thenReturn(Collections.emptyList());

        simulationService.simulateMarketTick();

        verify(stockRepository, times(1)).findAll();
        verify(stockRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Catches and logs unexpected database exceptions without throwing")
    void testExceptionHandledGracefully() {
        when(stockRepository.findAll()).thenThrow(new RuntimeException("Database connection timeout"));

        // Should not throw exception
        simulationService.simulateMarketTick();

        verify(stockRepository, times(1)).findAll();
        verify(stockRepository, never()).saveAll(any());
    }
}