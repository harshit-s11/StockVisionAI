package com.stockvision.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockvision.client.GeminiClient;
import com.stockvision.dto.AiChatRequest;
import com.stockvision.dto.TradeRequest;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityBoundaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @MockBean
    private GeminiClient geminiClient;

    private UserEntity userA;
    private UserEntity userB;
    private PortfolioEntity portfolioA;
    private PortfolioEntity portfolioB;
    private StockEntity tcs;
    private StockEntity infy;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        holdingRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();
        stockRepository.deleteAll();

        userA = userRepository.save(new UserEntity("alice", "alice@example.com", "pass", Role.ROLE_USER));
        portfolioA = portfolioRepository.save(new PortfolioEntity(userA, new BigDecimal("60000.00")));

        userB = userRepository.save(new UserEntity("bob", "bob@example.com", "pass", Role.ROLE_USER));
        portfolioB = portfolioRepository.save(new PortfolioEntity(userB, new BigDecimal("90000.00")));

        tcs = stockRepository.save(new StockEntity("TCS", "Tata Consultancy Services Ltd.", "IT",
            new BigDecimal("3850.00"), new BigDecimal("3810.00")));
        infy = stockRepository.save(new StockEntity("INFY", "Infosys Ltd.", "IT",
            new BigDecimal("1620.00"), new BigDecimal("1605.00")));

        tokenA = jwtTokenProvider.generateToken(userA.getId(), userA.getUsername(), userA.getRole());
        tokenB = jwtTokenProvider.generateToken(userB.getId(), userB.getUsername(), userB.getRole());

        when(geminiClient.generateText(anyString(), anyString()))
            .thenReturn("Mock advisory answer.");
        when(geminiClient.generateEmbedding(anyString()))
            .thenReturn(new float[768]);
    }

    // =========================================================================
    // 1. UNAUTHENTICATED ACCESS BOUNDARY (Must return 401 on all protected endpoints)
    // =========================================================================

    @Test
    @DisplayName("Unauthenticated requests to all protected APIs should strictly return 401 Unauthorized")
    void testUnauthenticatedAccessRejectedAcrossAllProtectedEndpoints() throws Exception {
        // Portfolio summary
        mockMvc.perform(get("/api/v1/portfolio"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.error").value("Unauthorized"));

        // Portfolio holdings
        mockMvc.perform(get("/api/v1/portfolio/holdings"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        // Transactions
        mockMvc.perform(get("/api/v1/transactions"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        // Trade Buy
        mockMvc.perform(post("/api/v1/trades/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TradeRequest("TCS", 1))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        // Trade Sell
        mockMvc.perform(post("/api/v1/trades/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TradeRequest("TCS", 1))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        // AI Chat
        mockMvc.perform(post("/api/v1/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AiChatRequest("test"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        // AI Stock Analysis
        mockMvc.perform(post("/api/v1/ai/analyze-stock")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symbol\":\"TCS\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    // =========================================================================
    // 2. INVALID / MALFORMED / TAMPERED TOKEN BOUNDARY (Must return 401)
    // =========================================================================

    @Test
    @DisplayName("Invalid or tampered JWT tokens should strictly return 401 Unauthorized")
    void testInvalidOrTamperedTokenRejected() throws Exception {
        // Malformed token
        mockMvc.perform(get("/api/v1/portfolio")
                .header("Authorization", "Bearer invalid.jwt.token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        // Tampered token (modified signature)
        String tamperedToken = tokenA.substring(0, tokenA.length() - 5) + "abcde";
        mockMvc.perform(get("/api/v1/portfolio")
                .header("Authorization", "Bearer " + tamperedToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        // Non-bearer authorization header
        mockMvc.perform(get("/api/v1/portfolio")
                .header("Authorization", "Basic dXNlcjpwYXNz"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    // =========================================================================
    // 3. PUBLIC ENDPOINT ACCESSIBILITY (Must remain accessible without auth)
    // =========================================================================

    @Test
    @DisplayName("Public endpoints should remain accessible without authentication")
    void testPublicEndpointsAccessibleWithoutAuth() throws Exception {
        // Health check
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));

        // Actuator health
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());

        // Market data list
        mockMvc.perform(get("/api/v1/stocks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        // Market data quote
        mockMvc.perform(get("/api/v1/stocks/TCS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("TCS"));
    }

    // =========================================================================
    // 4. CROSS-USER TRADE ISOLATION (User A cannot mutate User B)
    // =========================================================================

    @Test
    @DisplayName("User A executing BUY trade cannot affect User B cash, holdings, or transactions")
    void testCrossUserBuyIsolation() throws Exception {
        // Baseline: User A cash = 60,000; User B cash = 90,000
        // User A buys 10 shares of TCS @ 3850 (Cost: 38,500)
        TradeRequest request = new TradeRequest("TCS", 10);

        mockMvc.perform(post("/api/v1/trades/buy")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cashBalance").value(21500.00));

        // User A state mutated correctly
        PortfolioEntity updatedA = portfolioRepository.findById(portfolioA.getId()).orElseThrow();
        assertThat(updatedA.getCashBalance()).isEqualByComparingTo(new BigDecimal("21500.00"));
        assertThat(holdingRepository.findByPortfolioId(portfolioA.getId())).hasSize(1);
        assertThat(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioA.getId())).hasSize(1);

        // User B state remains 100% untouched
        PortfolioEntity untouchedB = portfolioRepository.findById(portfolioB.getId()).orElseThrow();
        assertThat(untouchedB.getCashBalance()).isEqualByComparingTo(new BigDecimal("90000.00"));
        assertThat(holdingRepository.findByPortfolioId(portfolioB.getId())).isEmpty();
        assertThat(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioB.getId())).isEmpty();
    }

    @Test
    @DisplayName("User A cannot sell shares owned only by User B")
    void testCrossUserSellUnauthorizedHolding() throws Exception {
        // Setup: User B owns 10 shares of TCS; User A owns 0 shares of TCS
        holdingRepository.save(new HoldingEntity(portfolioB, tcs, 10, new BigDecimal("3850.00")));

        // User A attempts to SELL 5 shares of TCS
        TradeRequest request = new TradeRequest("TCS", 5);

        mockMvc.perform(post("/api/v1/trades/sell")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value(containsString("Insufficient holdings")));

        // User B's holding is untouched (still 10 shares)
        HoldingEntity bHolding = holdingRepository.findByPortfolioIdAndStockId(portfolioB.getId(), tcs.getId()).orElseThrow();
        assertThat(bHolding.getQuantity()).isEqualTo(10);

        // User A has 0 holdings and 0 transactions
        assertThat(holdingRepository.findByPortfolioId(portfolioA.getId())).isEmpty();
        assertThat(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioA.getId())).isEmpty();
    }

    // =========================================================================
    // 5. CROSS-USER DATA ACCESS RESTRICTION
    // =========================================================================

    @Test
    @DisplayName("User A can only access User A's data; User B can only access User B's data")
    void testStrictSymmetricDataAccess() throws Exception {
        // Setup: User A owns TCS, User B owns INFY
        holdingRepository.save(new HoldingEntity(portfolioA, tcs, 5, new BigDecimal("3850.00")));
        holdingRepository.save(new HoldingEntity(portfolioB, infy, 20, new BigDecimal("1620.00")));

        transactionRepository.save(new TransactionEntity(portfolioA, tcs, TransactionType.BUY, 5,
            new BigDecimal("3850.00"), new BigDecimal("19250.00")));
        transactionRepository.save(new TransactionEntity(portfolioB, infy, TransactionType.BUY, 20,
            new BigDecimal("1620.00"), new BigDecimal("32400.00")));

        // User A reads holdings: sees ONLY TCS
        mockMvc.perform(get("/api/v1/portfolio/holdings")
                .header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].symbol").value("TCS"))
            .andExpect(jsonPath("$[0].quantity").value(5));

        // User B reads holdings: sees ONLY INFY
        mockMvc.perform(get("/api/v1/portfolio/holdings")
                .header("Authorization", "Bearer " + tokenB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].symbol").value("INFY"))
            .andExpect(jsonPath("$[0].quantity").value(20));

        // User A reads transactions: sees ONLY TCS
        mockMvc.perform(get("/api/v1/transactions")
                .header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].symbol").value("TCS"));

        // User B reads transactions: sees ONLY INFY
        mockMvc.perform(get("/api/v1/transactions")
                .header("Authorization", "Bearer " + tokenB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].symbol").value("INFY"));
    }
}