package com.stockvision.service;

import com.stockvision.config.GeminiProperties;
import com.stockvision.entity.FinancialKnowledgeDocEntity;
import com.stockvision.repository.FinancialKnowledgeDocRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private FinancialKnowledgeDocRepository docRepository;

    private GeminiProperties geminiProperties;
    private RagService ragService;

    @BeforeEach
    void setUp() {
        geminiProperties = new GeminiProperties();
        geminiProperties.setApiKey("test-key");
        geminiProperties.setRagTopK(5);
        ragService = new RagService(embeddingService, docRepository, geminiProperties);
    }

    @Test
    @DisplayName("Should retrieve relevant knowledge using vector similarity query")
    void testRetrieveRelevantKnowledge() {
        float[] vector = new float[768];
        when(embeddingService.embedText("P/E ratio")).thenReturn(vector);

        FinancialKnowledgeDocEntity doc = new FinancialKnowledgeDocEntity(
            "P/E Ratio", "Content about P/E", 0, "Source", vector
        );
        when(docRepository.findSimilarDocuments(anyString(), eq(3)))
            .thenReturn(List.of(doc));

        List<FinancialKnowledgeDocEntity> results = ragService.retrieveRelevantKnowledge("P/E ratio", 3);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("P/E Ratio");
    }

    @Test
    @DisplayName("Should return empty list for blank query")
    void testBlankQueryReturnsEmptyList() {
        List<FinancialKnowledgeDocEntity> results = ragService.retrieveRelevantKnowledge("   ", 5);
        assertThat(results).isEmpty();
        verifyNoInteractions(embeddingService);
    }

    @Test
    @DisplayName("Should fallback to findAll when Gemini is unconfigured")
    void testFallbackWhenGeminiUnconfigured() {
        geminiProperties.setApiKey(null);
        FinancialKnowledgeDocEntity doc = new FinancialKnowledgeDocEntity(
            "Basic Doc", "Basic Content", 0, "Source", null
        );
        when(docRepository.findAll()).thenReturn(List.of(doc));

        List<FinancialKnowledgeDocEntity> results = ragService.retrieveRelevantKnowledge("portfolio", 3);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Basic Doc");
        verifyNoInteractions(embeddingService);
    }

    @Test
    @DisplayName("Should fallback to findAll gracefully when embeddingService throws exception")
    void testFallbackWhenEmbeddingFails() {
        when(embeddingService.embedText("portfolio")).thenThrow(new RuntimeException("Embedding generation failed"));
        FinancialKnowledgeDocEntity doc = new FinancialKnowledgeDocEntity(
            "Fallback Doc", "Fallback Content", 0, "Source", null
        );
        when(docRepository.findAll()).thenReturn(List.of(doc));

        List<FinancialKnowledgeDocEntity> results = ragService.retrieveRelevantKnowledge("portfolio", 3);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Fallback Doc");
    }
}