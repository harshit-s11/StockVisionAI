package com.stockvision.controller;

import com.stockvision.entity.*;
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
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UserEntity user;
    private PortfolioEntity portfolio;
    private StockEntity stock;
    private String token;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();
        stockRepository.deleteAll();

        user = userRepository.save(new UserEntity("tx_user", "tx@example.com", "pass", Role.ROLE_USER));
        portfolio = portfolioRepository.save(new PortfolioEntity(user, new BigDecimal("100000.00")));
        stock = stockRepository.save(new StockEntity("TCS", "Tata Consultancy Services Ltd.", "IT",
            new BigDecimal("3850.00"), new BigDecimal("3810.00")));

        token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
    }

    @Test
    @DisplayName("GET /api/v1/transactions without token should return 401")
    void testGetTransactionsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/transactions"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/transactions with no transactions should return empty array")
    void testGetTransactionsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/transactions")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/transactions should return authenticated user's transactions sorted descending")
    void testGetTransactionsSuccess() throws Exception {
        transactionRepository.save(new TransactionEntity(portfolio, stock, TransactionType.BUY, 5,
            new BigDecimal("3800.00"), new BigDecimal("19000.00")));
        transactionRepository.save(new TransactionEntity(portfolio, stock, TransactionType.BUY, 10,
            new BigDecimal("3850.00"), new BigDecimal("38500.00")));

        mockMvc.perform(get("/api/v1/transactions")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].symbol").value("TCS"))
            .andExpect(jsonPath("$[0].type").value("BUY"))
            .andExpect(jsonPath("$[0].totalValue").isNotEmpty());
    }
}