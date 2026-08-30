package com.stockvision.controller;

import com.stockvision.entity.StockEntity;
import com.stockvision.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MarketDataControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void setUp() {
        stockRepository.deleteAll();
        stockRepository.save(new StockEntity("TCS", "Tata Consultancy Services Ltd.", "Information Technology",
            new BigDecimal("3850.00"), new BigDecimal("3810.00")));
        stockRepository.save(new StockEntity("INFY", "Infosys Ltd.", "Information Technology",
            new BigDecimal("1620.00"), new BigDecimal("1605.00")));
    }

    @Test
    @DisplayName("GET /api/v1/stocks should be publicly accessible and return demo stocks")
    void testGetAllStocksPublicSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/stocks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].dataStatus").value("DEMO"))
            .andExpect(jsonPath("$[0].symbol").value(anyOf(is("TCS"), is("INFY"))));
    }

    @Test
    @DisplayName("GET /api/v1/stocks/{symbol} should return stock by symbol with accurate price change")
    void testGetStockBySymbolSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/tcs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("TCS"))
            .andExpect(jsonPath("$.companyName").value("Tata Consultancy Services Ltd."))
            .andExpect(jsonPath("$.currentPrice").value(3850.00))
            .andExpect(jsonPath("$.previousClose").value(3810.00))
            .andExpect(jsonPath("$.change").value(40.00))
            .andExpect(jsonPath("$.changePercent").value(1.05))
            .andExpect(jsonPath("$.dataStatus").value("DEMO"));
    }

    @Test
    @DisplayName("GET /api/v1/stocks/{symbol} should return 404 for nonexistent symbol")
    void testGetStockBySymbolNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/NONEXISTENT"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value(containsString("Stock not found with symbol: NONEXISTENT")));
    }
}