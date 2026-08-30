package com.stockvision.service;

import com.stockvision.config.GeminiProperties;
import com.stockvision.entity.FinancialKnowledgeDocEntity;
import com.stockvision.entity.VectorConverter;
import com.stockvision.repository.FinancialKnowledgeDocRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RagService {

    private final EmbeddingService embeddingService;
    private final FinancialKnowledgeDocRepository docRepository;
    private final GeminiProperties geminiProperties;
    private final VectorConverter vectorConverter = new VectorConverter();

    public RagService(EmbeddingService embeddingService,
                      FinancialKnowledgeDocRepository docRepository,
                      GeminiProperties geminiProperties) {
        this.embeddingService = embeddingService;
        this.docRepository = docRepository;
        this.geminiProperties = geminiProperties;
    }

    public List<FinancialKnowledgeDocEntity> retrieveRelevantKnowledge(String query, Integer topK) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        int limit = (topK != null && topK > 0) ? topK : geminiProperties.getRagTopK();

        if (!geminiProperties.isConfigured()) {
            return docRepository.findAll().stream().limit(limit).toList();
        }

        try {
            float[] queryVector = embeddingService.embedText(query);
            String vectorStr = vectorConverter.convertToDatabaseColumn(queryVector);
            return docRepository.findSimilarDocuments(vectorStr, limit);
        } catch (Exception ex) {
            // Fallback gracefully to non-vector retrieval if embedding fails or during tests
            return docRepository.findAll().stream().limit(limit).toList();
        }
    }
}