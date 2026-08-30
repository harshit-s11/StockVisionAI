package com.stockvision.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockvision.client.GeminiClient;
import com.stockvision.dto.AiChatRequest;
import com.stockvision.entity.PortfolioEntity;
import com.stockvision.entity.Role;
import com.stockvision.entity.StockEntity;
import com.stockvision.entity.UserEntity;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AiTradingSafetyBoundaryTest {

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

    private UserEntity user;
    private PortfolioEntity portfolio;
    private String token;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        holdingRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();
        stockRepository.deleteAll();

        user = userRepository.save(new UserEntity("safe_user", "safe@example.com", "pass", Role.ROLE_USER));
        portfolio = portfolioRepository.save(new PortfolioEntity(user, new BigDecimal("100000.00")));
        stockRepository.save(new StockEntity("TCS", "Tata Consultancy Services Ltd.", "IT",
            new BigDecimal("3850.00"), new BigDecimal("3810.00")));

        token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        when(geminiClient.generateText(anyString(), anyString()))
            .thenReturn("I am an advisory AI and cannot execute trades. Please use the platform trading interface.");
        when(geminiClient.generateEmbedding(anyString()))
            .thenReturn(new float[768]);
    }

    @Test
    @DisplayName("AI Trading Boundary: Attempting to buy stocks via AI chat must not mutate database or deduct cash")
    void testAiCannotExecuteTrade() throws Exception {
        AiChatRequest request = new AiChatRequest("Buy 10 shares of TCS for me immediately");

        var result = mockMvc.perform(post("/api/v1/ai/chat")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        // 1. Verify cash balance is untouched
        PortfolioEntity p = portfolioRepository.findById(portfolio.getId()).orElseThrow();
        assertThat(p.getCashBalance()).isEqualByComparingTo(new BigDecimal("100000.00"));

        // 2. Verify zero holdings created
        assertThat(holdingRepository.findByPortfolioId(portfolio.getId())).isEmpty();

        // 3. Verify zero transactions created
        assertThat(transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId())).isEmpty();

        // 4. Verify no secrets leaked in response
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("test-gemini-key");
        assertThat(responseBody).doesNotContain("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970337336763979244226452948404D6351655468576D5A7134743777217A25432A");
    }
}