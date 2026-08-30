package com.stockvision.service;

import com.stockvision.dto.TradeRequest;
import com.stockvision.dto.TradeResponse;
import com.stockvision.entity.PortfolioEntity;
import com.stockvision.entity.Role;
import com.stockvision.entity.StockEntity;
import com.stockvision.entity.UserEntity;
import com.stockvision.repository.HoldingRepository;
import com.stockvision.repository.PortfolioRepository;
import com.stockvision.repository.StockRepository;
import com.stockvision.repository.TransactionRepository;
import com.stockvision.repository.UserRepository;
import com.stockvision.security.CustomUserDetailsService;
import com.stockvision.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TradingConcurrencyIntegrationTest {

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
    private StockEntity tcs;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        holdingRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();
        stockRepository.deleteAll();

        user = userRepository.save(new UserEntity("concurrent_trader", "concurrent@example.com", "pass", Role.ROLE_USER));
        portfolio = portfolioRepository.save(new PortfolioEntity(user, new BigDecimal("100000.00")));
        tcs = stockRepository.save(new StockEntity("TCS", "Tata Consultancy Services Ltd.", "IT",
            new BigDecimal("3850.00"), new BigDecimal("3810.00")));
    }

    @Test
    @DisplayName("Concurrency Test: Two concurrent BUY trades of 10 shares each must result in exactly 20 shares and 23,000 cash")
    void testConcurrentBuyTrades() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        Callable<TradeResponse> task = () -> {
            // Set security context for thread
            var userDetails = userDetailsService.loadUserById(user.getId());
            var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            startLatch.await();
            try {
                return tradingService.executeBuy(new TradeRequest("TCS", 10));
            } finally {
                doneLatch.countDown();
                SecurityContextHolder.clearContext();
            }
        };

        Future<TradeResponse> future1 = executor.submit(task);
        Future<TradeResponse> future2 = executor.submit(task);

        // Start both tasks simultaneously
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);

        TradeResponse res1 = future1.get();
        TradeResponse res2 = future2.get();

        assertThat(res1).isNotNull();
        assertThat(res2).isNotNull();

        PortfolioEntity finalPortfolio = portfolioRepository.findById(portfolio.getId()).orElseThrow();
        // Initial: 100,000 - (10 * 3850 = 38,500) - (10 * 3850 = 38,500) = 23,000.00
        assertThat(finalPortfolio.getCashBalance()).isEqualByComparingTo(new BigDecimal("23000.00"));

        var finalHolding = holdingRepository.findByPortfolioIdAndStockId(portfolio.getId(), tcs.getId()).orElseThrow();
        assertThat(finalHolding.getQuantity()).isEqualTo(20);
        assertThat(finalHolding.getAverageBuyPrice()).isEqualByComparingTo(new BigDecimal("3850.00"));

        var txs = transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId());
        assertThat(txs).hasSize(2);

        executor.shutdown();
    }
}