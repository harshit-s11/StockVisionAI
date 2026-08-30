package com.stockvision.service;

import com.stockvision.client.GeminiClient;
import com.stockvision.config.GeminiProperties;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

    private final GeminiClient geminiClient;
    private final GeminiProperties geminiProperties;

    public EmbeddingService(GeminiClient geminiClient, GeminiProperties geminiProperties) {
        this.geminiClient = geminiClient;
        this.geminiProperties = geminiProperties;
    }

    public float[] embedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text for embedding must not be null or empty");
        }

        float[] embedding = geminiClient.generateEmbedding(text.trim());

        if (embedding == null || embedding.length != geminiProperties.getEmbeddingDimension()) {
            int actualDim = embedding != null ? embedding.length : 0;
            throw new IllegalStateException("Embedding dimension mismatch. Expected: " 
                + geminiProperties.getEmbeddingDimension() + ", Actual: " + actualDim);
        }

        return embedding;
    }
}