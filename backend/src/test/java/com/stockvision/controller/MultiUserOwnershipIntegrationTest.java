package com.stockvision.controller;

import com.stockvision.entity.*;
import com.stockvision.repository.HoldingRepository;
import com.stockvision.repository.PortfolioRepository;
import com.stockvision.repository.StockRepository;
import com.stockvision.repository.TransactionRepository;
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
class MultiUserOwnershipIntegrationTest {

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
    private TransactionRepository transactionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UserEntity userA;
    private UserEntity userB;
    private String tokenA;
    private String tokenB;
    private StockEntity tcs;
    private StockEntity infy;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        holdingRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();
        stockRepository.deleteAll();

        // 1. Create User A with ₹50,000 cash and TCS holding
        userA = userRepository.save(new UserEntity("alice", "alice@example.com", "pass", Role.ROLE_USER));
        PortfolioEntity portfolioA = portfolioRepository.save(new PortfolioEntity(userA, new BigDecimal("50000.00")));

        // 2. Create User B with ₹75,000 cash and INFY holding
        userB = userRepository.save(new UserEntity("bob", "bob@example.com", "pass", Role.ROLE_USER));
        PortfolioEntity portfolioB = portfolioRepository.save(new PortfolioEntity(userB, new BigDecimal("75000.00")));

        // 3. Create Stocks
        tcs = stockRepository.save(new StockEntity("TCS", "Tata Consultancy Services Ltd.", "IT",
            new BigDecimal("3850.00"), new BigDecimal("3810.00")));
        infy = stockRepository.save(new StockEntity("INFY", "Infosys Ltd.", "IT",
            new BigDecimal("1620.00"), new BigDecimal("1605.00")));

        // 4. Assign Holdings
        holdingRepository.save(new HoldingEntity(portfolioA, tcs, 10, new BigDecimal("3800.00")));
        holdingRepository.save(new HoldingEntity(portfolioB, infy, 15, new BigDecimal("1600.00")));

        // 5. Assign Transactions
        transactionRepository.save(new TransactionEntity(portfolioA, tcs, TransactionType.BUY, 10,
            new BigDecimal("3800.00"), new BigDecimal("38000.00")));
        transactionRepository.save(new TransactionEntity(portfolioB, infy, TransactionType.BUY, 15,
            new BigDecimal("1600.00"), new BigDecimal("24000.00")));

        // 6. Generate Tokens
        tokenA = jwtTokenProvider.generateToken(userA.getId(), userA.getUsername(), userA.getRole());
        tokenB = jwtTokenProvider.generateToken(userB.getId(), userB.getUsername(), userB.getRole());
    }

    @Test
    @DisplayName("User A and User B should see their own portfolio summary strictly isolated")
    void testStrictPortfolioSummaryIsolation() throws Exception {
        // User A query
        mockMvc.perform(get("/api/v1/portfolio")
                .header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashBalance").value(50000.00))
            .andExpect(jsonPath("$.totalInvested").value(38000.00))
            .andExpect(jsonPath("$.currentHoldingsValue").value(38500.00))
            .andExpect(jsonPath("$.totalPortfolioValue").value(88500.00));

        // User B query
        mockMvc.perform(get("/api/v1/portfolio")
                .header("Authorization", "Bearer " + tokenB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashBalance").value(75000.00))
            .andExpect(jsonPath("$.totalInvested").value(24000.00))
            .andExpect(jsonPath("$.currentHoldingsValue").value(24300.00))
            .andExpect(jsonPath("$.totalPortfolioValue").value(99300.00));
    }

    @Test
    @DisplayName("User A cannot see User B holdings and vice-versa")
    void testStrictHoldingsIsolation() throws Exception {
        // User A sees ONLY TCS
        mockMvc.perform(get("/api/v1/portfolio/holdings")
                .header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].symbol").value("TCS"))
            .andExpect(jsonPath("$[0].quantity").value(10));

        // User B sees ONLY INFY
        mockMvc.perform(get("/api/v1/portfolio/holdings")
                .header("Authorization", "Bearer " + tokenB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].symbol").value("INFY"))
            .andExpect(jsonPath("$[0].quantity").value(15));
    }

    @Test
    @DisplayName("User A cannot see User B transactions and vice-versa")
    void testStrictTransactionsIsolation() throws Exception {
        // User A sees ONLY TCS transaction
        mockMvc.perform(get("/api/v1/transactions")
                .header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].symbol").value("TCS"))
            .andExpect(jsonPath("$[0].quantity").value(10));

        // User B sees ONLY INFY transaction
        mockMvc.perform(get("/api/v1/transactions")
                .header("Authorization", "Bearer " + tokenB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].symbol").value("INFY"))
            .andExpect(jsonPath("$[0].quantity").value(15));
    }
}