package com.stockvision.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TradeControllerIntegrationTest {

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

    private UserEntity user;
    private PortfolioEntity portfolio;
    private StockEntity tcs;
    private StockEntity infy;
    private String token;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        holdingRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();
        stockRepository.deleteAll();

        user = userRepository.save(new UserEntity("trader_pro", "trader@example.com", "pass", Role.ROLE_USER));
        portfolio = portfolioRepository.save(new PortfolioEntity(user, new BigDecimal("100000.00")));

        tcs = stockRepository.save(new StockEntity("TCS", "Tata Consultancy Services Ltd.", "IT",
            new BigDecimal("3850.00"), new BigDecimal("3810.00")));
        infy = stockRepository.save(new StockEntity("INFY", "Infosys Ltd.", "IT",
            new BigDecimal("1620.00"), new BigDecimal("1605.00")));

        token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
    }

    // ==========================================
    // POSITIVE LIFECYCLE TESTS
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/trades/buy should execute BUY, deduct cash, create holding and transaction")
    void testBuySuccess() throws Exception {
        TradeRequest request = new TradeRequest("TCS", 10);

        mockMvc.perform(post("/api/v1/trades/buy")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transactionId").isNotEmpty())
            .andExpect(jsonPath("$.symbol").value("TCS"))
            .andExpect(jsonPath("$.type").value("BUY"))
            .andExpect(jsonPath("$.quantity").value(10))
            .andExpect(jsonPath("$.executionPrice").value(3850.00))
            .andExpect(jsonPath("$.totalValue").value(38500.00))
            .andExpect(jsonPath("$.cashBalance").value(61500.00))
            .andExpect(jsonPath("$.holdingQuantity").value(10))
            .andExpect(jsonPath("$.averageBuyPrice").value(3850.00));

        PortfolioEntity updatedPortfolio = portfolioRepository.findById(portfolio.getId()).orElseThrow();
        assertThat(updatedPortfolio.getCashBalance()).isEqualByComparingTo(new BigDecimal("61500.00"));

        Optional<HoldingEntity> holding = holdingRepository.findByPortfolioIdAndStockId(portfolio.getId(), tcs.getId());
        assertThat(holding).isPresent();
        assertThat(holding.get().getQuantity()).isEqualTo(10);
        assertThat(holding.get().getAverageBuyPrice()).isEqualByComparingTo(new BigDecimal("3850.00"));

        List<TransactionEntity> txs = transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId());
        assertThat(txs).hasSize(1);
        assertThat(txs.get(0).getType()).isEqualTo(TransactionType.BUY);
    }

    @Test
    @DisplayName("POST /api/v1/trades/buy with multiple buys should recalculate weighted average buy price correctly")
    void testBuyWeightedAverageRecalculation() throws Exception {
        // Buy 1: 10 shares @ 3850 = 38,500
        mockMvc.perform(post("/api/v1/trades/buy")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TradeRequest("TCS", 10))))
            .andExpect(status().isOk());

        // Update market price to 4000
        tcs.setCurrentPrice(new BigDecimal("4000.00"));
        stockRepository.save(tcs);

        // Buy 2: 10 shares @ 4000 = 40,000 (Total cost: 78,500 / 20 shares = 3925.00 average)
        mockMvc.perform(post("/api/v1/trades/buy")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TradeRequest("TCS", 10))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.holdingQuantity").value(20))
            .andExpect(jsonPath("$.averageBuyPrice").value(3925.00))
            .andExpect(jsonPath("$.cashBalance").value(21500.00));
    }

    @Test
    @DisplayName("POST /api/v1/trades/sell partial quantity should credit cash and keep averageBuyPrice unchanged")
    void testSellPartialSuccess() throws Exception {
        // Setup initial holding: 10 shares @ 3850, Cash: 50,000
        holdingRepository.save(new HoldingEntity(portfolio, tcs, 10, new BigDecimal("3850.00")));
        portfolio.setCashBalance(new BigDecimal("50000.00"));
        portfolioRepository.save(portfolio);

        // Sell 4 shares @ 3850 = +15,400 credit -> New cash: 65,400, Remaining shares: 6
        TradeRequest request = new TradeRequest("TCS", 4);

        mockMvc.perform(post("/api/v1/trades/sell")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("SELL"))
            .andExpect(jsonPath("$.quantity").value(4))
            .andExpect(jsonPath("$.executionPrice").value(3850.00))
            .andExpect(jsonPath("$.totalValue").value(15400.00))
            .andExpect(jsonPath("$.cashBalance").value(65400.00))
            .andExpect(jsonPath("$.holdingQuantity").value(6))
            .andExpect(jsonPath("$.averageBuyPrice").value(3850.00));

        HoldingEntity updatedHolding = holdingRepository.findByPortfolioIdAndStockId(portfolio.getId(), tcs.getId()).orElseThrow();
        assertThat(updatedHolding.getQuantity()).isEqualTo(6);
        assertThat(updatedHolding.getAverageBuyPrice()).isEqualByComparingTo(new BigDecimal("3850.00"));
    }

    @Test
    @DisplayName("POST /api/v1/trades/sell complete holding should delete holding and preserve transaction")
    void testSellCompleteHoldingDeletesHolding() throws Exception {
        holdingRepository.save(new HoldingEntity(portfolio, tcs, 10, new BigDecimal("3850.00")));
        portfolio.setCashBalance(new BigDecimal("50000.00"));
        portfolioRepository.save(portfolio);

        // Sell all 10 shares
        TradeRequest request = new TradeRequest("TCS", 10);

        mockMvc.perform(post("/api/v1/trades/sell")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.holdingQuantity").value(0))
            .andExpect(jsonPath("$.cashBalance").value(88500.00));

        Optional<HoldingEntity> deletedHolding = holdingRepository.findByPortfolioIdAndStockId(portfolio.getId(), tcs.getId());
        assertThat(deletedHolding).isEmpty();

        List<TransactionEntity> txs = transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId());
        assertThat(txs).hasSize(1);
        assertThat(txs.get(0).getType()).isEqualTo(TransactionType.SELL);
    }

    @Test
    @DisplayName("Server Price Test: Execution price must strictly be resolved from database stock price")
    void testServerPriceResolutionMandatory() throws Exception {
        TradeRequest request = new TradeRequest("INFY", 2);

        mockMvc.perform(post("/api/v1/trades/buy")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.executionPrice").value(1620.00))
            .andExpect(jsonPath("$.totalValue").value(3240.00));
    }

    // ==========================================
    // NEGATIVE & VALIDATION INTEGRATION TESTS
    // ==========================================

    @Test
    @DisplayName("Negative Test: BUY with insufficient cash should reject with 400 and leave cash, holdings, and txs unchanged")
    void testBuyInsufficientCash_RejectsAndLeavesStateUnchanged() throws Exception {
        // Baseline: cash = 100,000.00, 0 holdings, 0 transactions
        BigDecimal initialCash = portfolio.getCashBalance();
        assertThat(initialCash).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(holdingRepository.findByPortfolioId(portfolio.getId())).isEmpty();
        assertThat(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId())).isEmpty();

        // Attempt BUY requiring 385,000.00 (100 shares @ 3850)
        TradeRequest request = new TradeRequest("TCS", 100);

        mockMvc.perform(post("/api/v1/trades/buy")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(containsString("Insufficient cash balance")));

        // Verify state is completely unchanged
        PortfolioEntity p = portfolioRepository.findById(portfolio.getId()).orElseThrow();
        assertThat(p.getCashBalance()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(holdingRepository.findByPortfolioId(portfolio.getId())).isEmpty();
        assertThat(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId())).isEmpty();
    }

    @Test
    @DisplayName("Negative Test: SELL more shares than owned should reject with 400 and leave cash, holdings, and txs unchanged")
    void testSellQuantityExceedsOwned_RejectsAndLeavesStateUnchanged() throws Exception {
        // Baseline: user owns 5 shares of TCS @ 3850, cash = 50,000.00, 0 transactions
        HoldingEntity initialHolding = holdingRepository.save(new HoldingEntity(portfolio, tcs, 5, new BigDecimal("3850.00")));
        portfolio.setCashBalance(new BigDecimal("50000.00"));
        portfolioRepository.save(portfolio);

        // Attempt SELL 10 shares (owns only 5)
        TradeRequest request = new TradeRequest("TCS", 10);

        mockMvc.perform(post("/api/v1/trades/sell")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(containsString("Insufficient holdings")));

        // Verify state is completely unchanged
        PortfolioEntity p = portfolioRepository.findById(portfolio.getId()).orElseThrow();
        assertThat(p.getCashBalance()).isEqualByComparingTo(new BigDecimal("50000.00"));

        HoldingEntity currentHolding = holdingRepository.findById(initialHolding.getId()).orElseThrow();
        assertThat(currentHolding.getQuantity()).isEqualTo(5);
        assertThat(currentHolding.getAverageBuyPrice()).isEqualByComparingTo(new BigDecimal("3850.00"));
        assertThat(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId())).isEmpty();
    }

    @Test
    @DisplayName("Negative Test: SELL a stock for which user has no holding should reject with 400 and leave cash, holdings, and txs unchanged")
    void testSellNotOwnedStock_RejectsAndLeavesStateUnchanged() throws Exception {
        // Baseline: user owns 5 shares of TCS @ 3850, cash = 50,000.00, 0 transactions
        HoldingEntity initialHolding = holdingRepository.save(new HoldingEntity(portfolio, tcs, 5, new BigDecimal("3850.00")));
        portfolio.setCashBalance(new BigDecimal("50000.00"));
        portfolioRepository.save(portfolio);

        // Attempt SELL 5 shares of INFY (owns 0 shares of INFY)
        TradeRequest request = new TradeRequest("INFY", 5);

        mockMvc.perform(post("/api/v1/trades/sell")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(containsString("Insufficient holdings")));

        // Verify state is completely unchanged
        PortfolioEntity p = portfolioRepository.findById(portfolio.getId()).orElseThrow();
        assertThat(p.getCashBalance()).isEqualByComparingTo(new BigDecimal("50000.00"));

        List<HoldingEntity> holdings = holdingRepository.findByPortfolioId(portfolio.getId());
        assertThat(holdings).hasSize(1);
        assertThat(holdings.get(0).getStock().getSymbol()).isEqualTo("TCS");
        assertThat(holdings.get(0).getQuantity()).isEqualTo(5);
        assertThat(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId())).isEmpty();
    }

    @Test
    @DisplayName("Validation Test: BUY with zero quantity should reject with 400 and leave cash, holdings, and txs unchanged")
    void testBuyZeroQuantity_RejectsAndLeavesStateUnchanged() throws Exception {
        TradeRequest request = new TradeRequest("TCS", 0);

        mockMvc.perform(post("/api/v1/trades/buy")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.validationErrors.quantity").isNotEmpty());

        PortfolioEntity p = portfolioRepository.findById(portfolio.getId()).orElseThrow();
        assertThat(p.getCashBalance()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(holdingRepository.findByPortfolioId(portfolio.getId())).isEmpty();
        assertThat(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId())).isEmpty();
    }

    @Test
    @DisplayName("Validation Test: BUY with negative quantity should reject with 400 and leave cash, holdings, and txs unchanged")
    void testBuyNegativeQuantity_RejectsAndLeavesStateUnchanged() throws Exception {
        TradeRequest request = new TradeRequest("TCS", -5);

        mockMvc.perform(post("/api/v1/trades/buy")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.validationErrors.quantity").isNotEmpty());

        PortfolioEntity p = portfolioRepository.findById(portfolio.getId()).orElseThrow();
        assertThat(p.getCashBalance()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(holdingRepository.findByPortfolioId(portfolio.getId())).isEmpty();
        assertThat(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId())).isEmpty();
    }

    @Test
    @DisplayName("Validation Test: SELL with zero quantity should reject with 400 and leave cash, holdings, and txs unchanged")
    void testSellZeroQuantity_RejectsAndLeavesStateUnchanged() throws Exception {
        // Baseline: user owns 5 shares of TCS
        HoldingEntity initialHolding = holdingRepository.save(new HoldingEntity(portfolio, tcs, 5, new BigDecimal("3850.00")));
        portfolio.setCashBalance(new BigDecimal("50000.00"));
        portfolioRepository.save(portfolio);

        TradeRequest request = new TradeRequest("TCS", 0);

        mockMvc.perform(post("/api/v1/trades/sell")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.validationErrors.quantity").isNotEmpty());

        PortfolioEntity p = portfolioRepository.findById(portfolio.getId()).orElseThrow();
        assertThat(p.getCashBalance()).isEqualByComparingTo(new BigDecimal("50000.00"));

        HoldingEntity currentHolding = holdingRepository.findById(initialHolding.getId()).orElseThrow();
        assertThat(currentHolding.getQuantity()).isEqualTo(5);
        assertThat(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId())).isEmpty();
    }

    @Test
    @DisplayName("Validation Test: SELL with negative quantity should reject with 400 and leave cash, holdings, and txs unchanged")
    void testSellNegativeQuantity_RejectsAndLeavesStateUnchanged() throws Exception {
        // Baseline: user owns 5 shares of TCS
        HoldingEntity initialHolding = holdingRepository.save(new HoldingEntity(portfolio, tcs, 5, new BigDecimal("3850.00")));
        portfolio.setCashBalance(new BigDecimal("50000.00"));
        portfolioRepository.save(portfolio);

        TradeRequest request = new TradeRequest("TCS", -3);

        mockMvc.perform(post("/api/v1/trades/sell")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.validationErrors.quantity").isNotEmpty());

        PortfolioEntity p = portfolioRepository.findById(portfolio.getId()).orElseThrow();
        assertThat(p.getCashBalance()).isEqualByComparingTo(new BigDecimal("50000.00"));

        HoldingEntity currentHolding = holdingRepository.findById(initialHolding.getId()).orElseThrow();
        assertThat(currentHolding.getQuantity()).isEqualTo(5);
        assertThat(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId())).isEmpty();
    }

    @Test
    @DisplayName("Negative Test: POST /api/v1/trades/buy with unknown stock should return 404 and make no mutations")
    void testBuyUnknownStock() throws Exception {
        TradeRequest request = new TradeRequest("UNKNOWN_TICKER", 1);

        mockMvc.perform(post("/api/v1/trades/buy")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"));

        PortfolioEntity p = portfolioRepository.findById(portfolio.getId()).orElseThrow();
        assertThat(p.getCashBalance()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(holdingRepository.findByPortfolioId(portfolio.getId())).isEmpty();
        assertThat(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId())).isEmpty();
    }
}