package com.stockvision.service;

import com.stockvision.config.MarketSimulationProperties;
import com.stockvision.dto.PortfolioSummaryResponse;
import com.stockvision.dto.TradeRequest;
import com.stockvision.dto.TradeResponse;
import com.stockvision.entity.PortfolioEntity;
import com.stockvision.entity.Role;
import com.stockvision.entity.StockEntity;
import com.stockvision.entity.UserEntity;
import com.stockvision.repository.HoldingRepository;
import com.stockvision.repository.PortfolioRepository;
import com.stockvision.repository.StockRepository;
import com.stockvision.repository.UserRepository;
import com.stockvision.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MarketSimulationIntegrationTest {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private TradingService tradingService;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private MarketSimulationService marketSimulationService;

    @Autowired
    private MarketSimulationProperties properties;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);

        testUser = userRepository.save(new UserEntity("sim_user", "sim@test.com", "hash", Role.ROLE_USER));
        portfolioRepository.save(new PortfolioEntity(testUser, new BigDecimal("100000.00")));

        UserDetails userDetails = userDetailsService.loadUserByUsername("sim_user");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Integration: Simulated price movement flows into Trading execution and Portfolio valuation")
    void testSimulationFlowsIntoTradingAndPortfolio() {
        // 1. Setup Stock TCS
        StockEntity stock = stockRepository.findBySymbolIgnoreCase("TCS")
            .orElseGet(() -> stockRepository.save(new StockEntity("TCS", "Tata Consultancy", "IT", new BigDecimal("3850.00"), new BigDecimal("3810.00"))));
        stock.setCurrentPrice(new BigDecimal("3850.00"));
        stockRepository.save(stock);

        // 2. User buys 10 shares of TCS at initial price (3850.00)
        TradeResponse buy1 = tradingService.executeBuy(new TradeRequest("TCS", 10));
        assertThat(buy1.getCashBalance()).isEqualByComparingTo("61500.00");
        assertThat(buy1.getAverageBuyPrice()).isEqualByComparingTo("3850.00");

        // 3. Verify initial portfolio summary
        PortfolioSummaryResponse summaryInitial = portfolioService.getPortfolioSummary();
        assertThat(summaryInitial.getCurrentHoldingsValue()).isEqualByComparingTo("38500.00");
        assertThat(summaryInitial.getUnrealizedPnL()).isEqualByComparingTo("0.00");
        assertThat(summaryInitial.getTotalPortfolioValue()).isEqualByComparingTo("100000.00");

        // 4. Trigger simulated market price increase to 4000.00
        stock.setCurrentPrice(new BigDecimal("4000.00"));
        stockRepository.save(stock);

        // 5. Verify portfolio summary automatically reflects the new simulated market price
        PortfolioSummaryResponse summaryAfterIncrease = portfolioService.getPortfolioSummary();
        // 10 shares * 4000 = 40,000 holdings value
        assertThat(summaryAfterIncrease.getCurrentHoldingsValue()).isEqualByComparingTo("40000.00");
        // 40,000 - 38,500 = +1,500.00 unrealized PnL
        assertThat(summaryAfterIncrease.getUnrealizedPnL()).isEqualByComparingTo("1500.00");
        // 61,500 cash + 40,000 holdings = 101,500.00 total value
        assertThat(summaryAfterIncrease.getTotalPortfolioValue()).isEqualByComparingTo("101500.00");

        // 6. User buys 1 more share at the new simulated price (4000.00)
        TradeResponse buy2 = tradingService.executeBuy(new TradeRequest("TCS", 1));
        assertThat(buy2.getExecutionPrice()).isEqualByComparingTo("4000.00");
        // 61,500 - 4,000 = 57,500
        assertThat(buy2.getCashBalance()).isEqualByComparingTo("57500.00");
        assertThat(buy2.getHoldingQuantity()).isEqualTo(11);
        // Weighted average: (38500 + 4000) / 11 = 3863.64
        assertThat(buy2.getAverageBuyPrice()).isEqualByComparingTo("3863.64");

        // 7. User sells 1 share at the current simulated price (4000.00)
        TradeResponse sell1 = tradingService.executeSell(new TradeRequest("TCS", 1));
        assertThat(sell1.getExecutionPrice()).isEqualByComparingTo("4000.00");
        // 57,500 + 4,000 = 61,500
        assertThat(sell1.getCashBalance()).isEqualByComparingTo("61500.00");
        assertThat(sell1.getHoldingQuantity()).isEqualTo(10);
        // Average buy price remains 3863.64 for remaining 10 shares
        assertThat(sell1.getAverageBuyPrice()).isEqualByComparingTo("3863.64");
    }
}