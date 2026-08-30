package com.stockvision.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockvision.client.GeminiClient;
import com.stockvision.dto.AiChatRequest;
import com.stockvision.entity.*;
import com.stockvision.repository.HoldingRepository;
import com.stockvision.repository.PortfolioRepository;
import com.stockvision.repository.StockRepository;
import com.stockvision.repository.UserRepository;
import com.stockvision.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CrossUserAiIsolationIntegrationTest {

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

    private UserEntity userA;
    private UserEntity userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        holdingRepository.deleteAll();
        portfolioRepository.deleteAll();
        userRepository.deleteAll();
        stockRepository.deleteAll();

        // User A: ₹40,000 cash and TCS holding
        userA = userRepository.save(new UserEntity("user_alice", "alice@example.com", "pass", Role.ROLE_USER));
        PortfolioEntity portfolioA = portfolioRepository.save(new PortfolioEntity(userA, new BigDecimal("40000.00")));

        // User B: ₹80,000 cash and INFY holding
        userB = userRepository.save(new UserEntity("user_bob", "bob@example.com", "pass", Role.ROLE_USER));
        PortfolioEntity portfolioB = portfolioRepository.save(new PortfolioEntity(userB, new BigDecimal("80000.00")));

        StockEntity tcs = stockRepository.save(new StockEntity("TCS", "Tata Consultancy Services Ltd.", "IT",
            new BigDecimal("3850.00"), new BigDecimal("3810.00")));
        StockEntity infy = stockRepository.save(new StockEntity("INFY", "Infosys Ltd.", "IT",
            new BigDecimal("1620.00"), new BigDecimal("1605.00")));

        holdingRepository.save(new HoldingEntity(portfolioA, tcs, 10, new BigDecimal("3850.00")));
        holdingRepository.save(new HoldingEntity(portfolioB, infy, 20, new BigDecimal("1620.00")));

        tokenA = jwtTokenProvider.generateToken(userA.getId(), userA.getUsername(), userA.getRole());
        tokenB = jwtTokenProvider.generateToken(userB.getId(), userB.getUsername(), userB.getRole());

        when(geminiClient.generateText(anyString(), anyString())).thenReturn("AI Educational Analysis");
        when(geminiClient.generateEmbedding(anyString())).thenReturn(new float[768]);
    }

    @Test
    @DisplayName("Cross-User Isolation: User A's prompt must contain only User A's data and not User B's")
    void testUserAPromptIsolation() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AiChatRequest("Review portfolio"))))
            .andExpect(status().isOk());

        ArgumentCaptor<String> userContextCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generateText(anyString(), userContextCaptor.capture());

        String capturedContext = userContextCaptor.getValue();
        assertThat(capturedContext).contains("40000.00");
        assertThat(capturedContext).contains("TCS");
        assertThat(capturedContext).doesNotContain("80000.00");
        assertThat(capturedContext).doesNotContain("INFY");
    }
}