package com.stockvision.service;

import com.stockvision.ai.tools.AiServerTools;
import com.stockvision.client.GeminiClient;
import com.stockvision.dto.*;
import com.stockvision.entity.FinancialKnowledgeDocEntity;
import com.stockvision.entity.HoldingEntity;
import com.stockvision.entity.PortfolioEntity;
import com.stockvision.entity.Role;
import com.stockvision.entity.StockEntity;
import com.stockvision.entity.UserEntity;
import com.stockvision.repository.HoldingRepository;
import com.stockvision.repository.PortfolioRepository;
import com.stockvision.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAdvisorServiceTest {

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private RagService ragService;

    @Mock
    private AiServerTools aiServerTools;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @InjectMocks
    private AiAdvisorService aiAdvisorService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        UserEntity user = new UserEntity("test_user", "test@example.com", "pass", Role.ROLE_USER);
        user.setId(userId);
        UserPrincipal principal = UserPrincipal.create(user);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("chat: deduplicates knowledge sources by title")
    void testChatDeduplicatesKnowledgeSources() {
        PortfolioSummaryResponse summary = new PortfolioSummaryResponse(
            new BigDecimal("100000.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            new BigDecimal("100000.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
        when(aiServerTools.getCurrentPortfolio()).thenReturn(summary);
        when(aiServerTools.getCurrentHoldings()).thenReturn(List.of());

        // Return docs with duplicate titles (e.g., from multiple chunk hits)
        FinancialKnowledgeDocEntity doc1 = new FinancialKnowledgeDocEntity("Diversification", "Chunk 1 content", 0, "Doc", new float[768]);
        FinancialKnowledgeDocEntity doc2 = new FinancialKnowledgeDocEntity("Diversification", "Chunk 2 content", 1, "Doc", new float[768]);
        FinancialKnowledgeDocEntity doc3 = new FinancialKnowledgeDocEntity("Risk Management", "Risk content", 0, "Doc", new float[768]);
        when(ragService.retrieveRelevantKnowledge(anyString(), anyInt())).thenReturn(List.of(doc1, doc2, doc3));

        when(geminiClient.generateText(anyString(), anyString())).thenReturn("### Portfolio Overview\n\nDirect answer here.");

        AiChatResponse response = aiAdvisorService.chat(new AiChatRequest("Explain portfolio diversification"));

        assertThat(response).isNotNull();
        assertThat(response.getAnswer()).contains("Portfolio Overview");
        // Verify sources are deduplicated: 3 docs become 2 unique titles
        assertThat(response.getSources()).hasSize(2);
        assertThat(response.getSources()).extracting(KnowledgeSourceResponse::getTitle)
            .containsExactly("Diversification", "Risk Management");

        // Verify aiServerTools.getRecentTransactions was NEVER called
        verify(aiServerTools, never()).getRecentTransactions();
    }

    @Test
    @DisplayName("chat: system instructions contain Markdown rules, completion requirement, and SVG prohibition")
    void testChatSystemInstructionContainsMarkdownAndSafetyRules() {
        PortfolioSummaryResponse summary = new PortfolioSummaryResponse(
            new BigDecimal("50000.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            new BigDecimal("50000.00"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
        when(aiServerTools.getCurrentPortfolio()).thenReturn(summary);
        when(aiServerTools.getCurrentHoldings()).thenReturn(List.of());
        when(ragService.retrieveRelevantKnowledge(anyString(), anyInt())).thenReturn(List.of());
        when(geminiClient.generateText(anyString(), anyString())).thenReturn("Response text");

        aiAdvisorService.chat(new AiChatRequest("How is my portfolio doing?"));

        ArgumentCaptor<String> systemInstructionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userContextCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generateText(systemInstructionCaptor.capture(), userContextCaptor.capture());

        String systemInstruction = systemInstructionCaptor.getValue();
        assertThat(systemInstruction).contains("RESPONSE FORMATTING & COMPLETION REQUIREMENTS");
        assertThat(systemInstruction).contains("standard Markdown only");
        assertThat(systemInstruction).contains("short, direct answer first");
        assertThat(systemInstruction).contains("Clear section headings (use ###)");
        assertThat(systemInstruction).contains("ALWAYS finish all sentences");
        assertThat(systemInstruction).contains("Never truncate or stop mid-sentence");
        assertThat(systemInstruction).contains("Never output raw HTML, XML, or SVG code");
        assertThat(systemInstruction).contains("Do not append bracketed source citations");
        assertThat(systemInstruction).contains("EDUCATIONAL ANALYSIS ONLY");

        String userContext = userContextCaptor.getValue();
        assertThat(userContext).contains("Cash Balance: ₹50000.00");
    }

    @Test
    @DisplayName("analyzeStock: provides structured stock analysis with deduplicated sources")
    void testAnalyzeStockDeduplicatesSources() {
        StockResponse stock = new StockResponse("INFY", "Infosys Ltd.", "Information Technology",
            new BigDecimal("1600.00"), new BigDecimal("1580.00"), new BigDecimal("20.00"),
            new BigDecimal("1.27"), "SIMULATED", Instant.now());
        when(aiServerTools.getStockQuote("INFY")).thenReturn(stock);

        UserEntity user = new UserEntity("test_user", "test@example.com", "pass", Role.ROLE_USER);
        PortfolioEntity portfolio = new PortfolioEntity(user, new BigDecimal("50000.00"));
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(holdingRepository.findByPortfolioIdAndStockSymbol(portfolio.getId(), "INFY")).thenReturn(Optional.empty());

        FinancialKnowledgeDocEntity doc1 = new FinancialKnowledgeDocEntity("Tech Sector Valuation", "P/E guide", 0, "Doc", new float[768]);
        FinancialKnowledgeDocEntity doc2 = new FinancialKnowledgeDocEntity("Tech Sector Valuation", "P/E guide part 2", 1, "Doc", new float[768]);
        when(ragService.retrieveRelevantKnowledge(anyString(), anyInt())).thenReturn(List.of(doc1, doc2));

        when(geminiClient.generateText(anyString(), anyString())).thenReturn("### Analysis\n\nInfosys shows solid fundamentals.");

        StockAnalysisResponse response = aiAdvisorService.analyzeStock(new StockAnalysisRequest("INFY"));

        assertThat(response.getSymbol()).isEqualTo("INFY");
        assertThat(response.getAnalysis()).contains("solid fundamentals");
        assertThat(response.getSources()).hasSize(1);
        assertThat(response.getSources().get(0).getTitle()).isEqualTo("Tech Sector Valuation");
        assertThat(response.getKeyFactors()).isNotEmpty();
        assertThat(response.getRisks()).isNotEmpty();
        assertThat(response.getDisclaimer()).isNotEmpty();
    }
}
