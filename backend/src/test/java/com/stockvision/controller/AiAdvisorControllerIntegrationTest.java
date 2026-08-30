package com.stockvision.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockvision.client.GeminiClient;
import com.stockvision.dto.AiChatRequest;
import com.stockvision.dto.StockAnalysisRequest;
import com.stockvision.entity.PortfolioEntity;
import com.stockvision.entity.Role;
import com.stockvision.entity.StockEntity;
import com.stockvision.entity.UserEntity;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AiAdvisorControllerIntegrationTest {

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
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private GeminiClient geminiClient;

    private UserEntity user;
    private PortfolioEntity portfolio;
    private StockEntity tcs;
    private String token;

    @BeforeEach
    void setUp() {
        holdingRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();
        stockRepository.deleteAll();

        user = userRepository.save(new UserEntity("ai_user", "ai@example.com", "pass", Role.ROLE_USER));
        portfolio = portfolioRepository.save(new PortfolioEntity(user, new BigDecimal("100000.00")));
        tcs = stockRepository.save(new StockEntity("TCS", "Tata Consultancy Services Ltd.", "IT",
            new BigDecimal("3850.00"), new BigDecimal("3810.00")));

        token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        when(geminiClient.generateText(anyString(), anyString()))
            .thenReturn("Your portfolio is well-balanced with ₹100,000 cash reserves.");
        when(geminiClient.generateEmbedding(anyString()))
            .thenReturn(new float[768]);
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat should return structured advisory response with disclaimer")
    void testChatEndpointSuccess() throws Exception {
        AiChatRequest request = new AiChatRequest("How diversified is my portfolio?");

        mockMvc.perform(post("/api/v1/ai/chat")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.answer").isNotEmpty())
            .andExpect(jsonPath("$.disclaimer").isNotEmpty())
            .andExpect(jsonPath("$.sources").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/ai/chat without token should return 401 Unauthorized")
    void testChatEndpointUnauthorized() throws Exception {
        AiChatRequest request = new AiChatRequest("Analyze portfolio");

        mockMvc.perform(post("/api/v1/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/v1/ai/analyze-stock should return structured stock analysis")
    void testAnalyzeStockSuccess() throws Exception {
        StockAnalysisRequest request = new StockAnalysisRequest("TCS");

        mockMvc.perform(post("/api/v1/ai/analyze-stock")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("TCS"))
            .andExpect(jsonPath("$.analysis").isNotEmpty())
            .andExpect(jsonPath("$.keyFactors").isArray())
            .andExpect(jsonPath("$.risks").isArray())
            .andExpect(jsonPath("$.disclaimer").isNotEmpty())
            .andExpect(jsonPath("$.sources").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/ai/analyze-stock for unknown symbol should return 404")
    void testAnalyzeStockNotFound() throws Exception {
        StockAnalysisRequest request = new StockAnalysisRequest("UNKNOWN_TICKER");

        mockMvc.perform(post("/api/v1/ai/analyze-stock")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }
}