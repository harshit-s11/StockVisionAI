package com.stockvision.controller;

import com.stockvision.entity.*;
import com.stockvision.repository.HoldingRepository;
import com.stockvision.repository.PortfolioRepository;
import com.stockvision.repository.StockRepository;
import com.stockvision.repository.UserRepository;
import com.stockvision.security.JwtTokenProvider;
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
class PortfolioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UserEntity testUser;
    private PortfolioEntity testPortfolio;
    private String token;

    @BeforeEach
    void setUp() {
        holdingRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();
        stockRepository.deleteAll();

        testUser = userRepository.save(new UserEntity("portfolio_trader", "trader@example.com", "pass", Role.ROLE_USER));
        testPortfolio = portfolioRepository.save(new PortfolioEntity(testUser, new BigDecimal("62000.00")));

        token = jwtTokenProvider.generateToken(testUser.getId(), testUser.getUsername(), testUser.getRole());
    }

    @Test
    @DisplayName("GET /api/v1/portfolio without token should return 401 Unauthorized")
    void testGetPortfolioUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/portfolio"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/portfolio with zero holdings should return clean summary")
    void testGetPortfolioZeroHoldings() throws Exception {
        testPortfolio.setCashBalance(new BigDecimal("100000.00"));
        portfolioRepository.save(testPortfolio);

        mockMvc.perform(get("/api/v1/portfolio")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashBalance").value(100000.00))
            .andExpect(jsonPath("$.totalInvested").value(0.00))
            .andExpect(jsonPath("$.currentHoldingsValue").value(0.00))
            .andExpect(jsonPath("$.totalPortfolioValue").value(100000.00))
            .andExpect(jsonPath("$.unrealizedPnL").value(0.00))
            .andExpect(jsonPath("$.unrealizedPnLPercent").value(0.00))
            .andExpect(jsonPath("$.portfolioReturnPercent").value(0.00));
    }

    @Test
    @DisplayName("GET /api/v1/portfolio with active holdings should calculate accurate valuation")
    void testGetPortfolioWithHoldings() throws Exception {
        StockEntity tcs = stockRepository.save(new StockEntity("TCS", "Tata Consultancy Services Ltd.", "IT",
            new BigDecimal("3850.00"), new BigDecimal("3810.00")));

        // 10 shares bought at 3800, current price 3850 (Cost: 38,000, Current: 38,500, Cash: 62,000)
        holdingRepository.save(new HoldingEntity(testPortfolio, tcs, 10, new BigDecimal("3800.00")));

        mockMvc.perform(get("/api/v1/portfolio")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashBalance").value(62000.00))
            .andExpect(jsonPath("$.totalInvested").value(38000.00))
            .andExpect(jsonPath("$.currentHoldingsValue").value(38500.00))
            .andExpect(jsonPath("$.totalPortfolioValue").value(100500.00))
            .andExpect(jsonPath("$.unrealizedPnL").value(500.00))
            .andExpect(jsonPath("$.unrealizedPnLPercent").value(1.32))
            .andExpect(jsonPath("$.portfolioReturnPercent").value(0.50));
    }

    @Test
    @DisplayName("GET /api/v1/portfolio/holdings should return holding details with allocation percentage")
    void testGetHoldingsSuccess() throws Exception {
        StockEntity tcs = stockRepository.save(new StockEntity("TCS", "Tata Consultancy Services Ltd.", "IT",
            new BigDecimal("3850.00"), new BigDecimal("3810.00")));

        holdingRepository.save(new HoldingEntity(testPortfolio, tcs, 10, new BigDecimal("3800.00")));

        mockMvc.perform(get("/api/v1/portfolio/holdings")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].symbol").value("TCS"))
            .andExpect(jsonPath("$[0].quantity").value(10))
            .andExpect(jsonPath("$[0].averageBuyPrice").value(3800.00))
            .andExpect(jsonPath("$[0].currentPrice").value(3850.00))
            .andExpect(jsonPath("$[0].currentValue").value(38500.00))
            .andExpect(jsonPath("$[0].unrealizedPnL").value(500.00))
            .andExpect(jsonPath("$[0].allocationPercent").value(100.00));
    }
}