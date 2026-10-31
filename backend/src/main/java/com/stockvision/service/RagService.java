package com.stockvision.service;

import com.stockvision.config.GeminiProperties;
import com.stockvision.entity.FinancialKnowledgeDocEntity;
import com.stockvision.entity.VectorConverter;
import com.stockvision.repository.FinancialKnowledgeDocRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final EmbeddingService embeddingService;
    private final FinancialKnowledgeDocRepository docRepository;
    private final GeminiProperties geminiProperties;
    private final RagKnowledgeSeeder ragKnowledgeSeeder;
    private final VectorConverter vectorConverter = new VectorConverter();

    @Autowired
    public RagService(EmbeddingService embeddingService,
                      FinancialKnowledgeDocRepository docRepository,
                      GeminiProperties geminiProperties,
                      @Lazy RagKnowledgeSeeder ragKnowledgeSeeder) {
        this.embeddingService = embeddingService;
        this.docRepository = docRepository;
        this.geminiProperties = geminiProperties;
        this.ragKnowledgeSeeder = ragKnowledgeSeeder;
    }

    public RagService(EmbeddingService embeddingService,
                      FinancialKnowledgeDocRepository docRepository,
                      GeminiProperties geminiProperties) {
        this(embeddingService, docRepository, geminiProperties, null);
    }

    public List<FinancialKnowledgeDocEntity> retrieveRelevantKnowledge(String query, Integer topK) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        if (ragKnowledgeSeeder != null) {
            try {
                if (docRepository.count() == 0) {
                    log.info("Knowledge document table is empty; triggering on-demand seeding");
                    ragKnowledgeSeeder.seedKnowledgeDocs();
                }
            } catch (Exception ex) {
                log.warn("On-demand knowledge document seeding failed: {}", ex.getMessage());
            }
        }

        int limit = (topK != null && topK > 0) ? topK : geminiProperties.getRagTopK();

        if (!geminiProperties.isConfigured()) {
            return docRepository.findAll().stream().limit(limit).toList();
        }

        try {
            float[] queryVector = embeddingService.embedText(query);
            String vectorStr = vectorConverter.toVectorString(queryVector);
            return docRepository.findSimilarDocuments(vectorStr, limit);
        } catch (Exception ex) {
            // Fallback gracefully to non-vector retrieval if embedding fails or during tests
            return docRepository.findAll().stream().limit(limit).toList();
        }
    }
}