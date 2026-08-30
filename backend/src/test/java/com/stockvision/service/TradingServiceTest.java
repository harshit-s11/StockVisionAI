package com.stockvision.service;

import com.stockvision.dto.TradeRequest;
import com.stockvision.dto.TradeResponse;
import com.stockvision.entity.*;
import com.stockvision.exception.InsufficientCashException;
import com.stockvision.exception.InsufficientHoldingsException;
import com.stockvision.repository.HoldingRepository;
import com.stockvision.repository.PortfolioRepository;
import com.stockvision.repository.StockRepository;
import com.stockvision.repository.TransactionRepository;
import com.stockvision.repository.UserRepository;
import com.stockvision.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TradingServiceTest {

    @Autowired
    private TradingService tradingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private UserEntity user;
    private PortfolioEntity portfolio;
    private StockEntity stock;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        holdingRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();
        stockRepository.deleteAll();

        user = userRepository.save(new UserEntity("svc_trader", "svc@example.com", "pass", Role.ROLE_USER));
        portfolio = portfolioRepository.save(new PortfolioEntity(user, new BigDecimal("100000.00")));
        stock = stockRepository.save(new StockEntity("RELIANCE", "Reliance Industries Ltd.", "Energy",
            new BigDecimal("2980.50"), new BigDecimal("2950.00")));

        var userDetails = userDetailsService.loadUserById(user.getId());
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Financial Precision: Weighted average buy price calculation with decimal numbers")
    void testFinancialPrecisionWeightedAverage() {
        // Buy 1: 3 shares @ 2980.50 = 8941.50
        TradeResponse res1 = tradingService.executeBuy(new TradeRequest("RELIANCE", 3));
        assertThat(res1.getCashBalance()).isEqualByComparingTo(new BigDecimal("91058.50"));
        assertThat(res1.getAverageBuyPrice()).isEqualByComparingTo(new BigDecimal("2980.50"));

        // Price changes to 3010.25
        stock.setCurrentPrice(new BigDecimal("3010.25"));
        stockRepository.save(stock);

        // Buy 2: 7 shares @ 3010.25 = 21071.75
        // Total cost: 8941.50 + 21071.75 = 30013.25
        // Total shares: 10
        // Expected average: 3001.325 -> 3001.33 (HALF_UP)
        TradeResponse res2 = tradingService.executeBuy(new TradeRequest("RELIANCE", 7));
        assertThat(res2.getCashBalance()).isEqualByComparingTo(new BigDecimal("69986.75"));
        assertThat(res2.getHoldingQuantity()).isEqualTo(10);
        assertThat(res2.getAverageBuyPrice()).isEqualByComparingTo(new BigDecimal("3001.33"));
    }

    @Test
    @DisplayName("Financial Precision: Partial sell preserves exact cost basis")
    void testFinancialPrecisionPartialSell() {
        // Buy 10 shares @ 2980.50
        tradingService.executeBuy(new TradeRequest("RELIANCE", 10));

        // Sell 3 shares @ 2980.50 = 8941.50
        TradeResponse res = tradingService.executeSell(new TradeRequest("RELIANCE", 3));
        assertThat(res.getCashBalance()).isEqualByComparingTo(new BigDecimal("79136.50"));
        assertThat(res.getHoldingQuantity()).isEqualTo(7);
        assertThat(res.getAverageBuyPrice()).isEqualByComparingTo(new BigDecimal("2980.50"));
    }

    @Test
    @DisplayName("Rollback verification: Insufficient cash rolls back completely and leaves no transaction or holding")
    void testRollbackOnInsufficientCash() {
        // Attempt buy requiring 298,050 when only 100,000 cash available
        assertThatThrownBy(() -> tradingService.executeBuy(new TradeRequest("RELIANCE", 100)))
            .isInstanceOf(InsufficientCashException.class);

        PortfolioEntity p = portfolioRepository.findById(portfolio.getId()).orElseThrow();
        assertThat(p.getCashBalance()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(holdingRepository.findByPortfolioId(portfolio.getId())).isEmpty();
        assertThat(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId())).isEmpty();
    }
}