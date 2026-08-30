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
}