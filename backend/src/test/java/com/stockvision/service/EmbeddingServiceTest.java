package com.stockvision.service;

import com.stockvision.client.GeminiClient;
import com.stockvision.config.GeminiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private GeminiClient geminiClient;

    private GeminiProperties geminiProperties;
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        geminiProperties = new GeminiProperties();
        geminiProperties.setEmbeddingDimension(768);
        embeddingService = new EmbeddingService(geminiClient, geminiProperties);
    }

    @Test
    @DisplayName("Should accept valid 768-dimensional embedding")
    void testValidEmbeddingDimension() {
        float[] validEmbedding = new float[768];
        validEmbedding[0] = 0.5f;
        validEmbedding[767] = -0.5f;

        when(geminiClient.generateEmbedding("Diversification")).thenReturn(validEmbedding);

        float[] result = embeddingService.embedText("Diversification");
        assertThat(result).hasSize(768);
        assertThat(result[0]).isEqualTo(0.5f);
    }

    @Test
    @DisplayName("Should reject embedding with dimension mismatch (fail-fast)")
    void testDimensionMismatch() {
        float[] invalidEmbedding = new float[512]; // Wrong dimension
        when(geminiClient.generateEmbedding("Invalid")).thenReturn(invalidEmbedding);

        assertThatThrownBy(() -> embeddingService.embedText("Invalid"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Embedding dimension mismatch");
    }

    @Test
    @DisplayName("Should reject null or empty text for embedding")
    void testNullOrEmptyText() {
        assertThatThrownBy(() -> embeddingService.embedText(null))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> embeddingService.embedText("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }
}