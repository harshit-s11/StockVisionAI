package com.stockvision.service;

import com.stockvision.config.GeminiProperties;
import com.stockvision.repository.FinancialKnowledgeDocRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class RagKnowledgeSeederTest {

    @Autowired
    private FinancialKnowledgeDocRepository docRepository;

    @Autowired
    private RagKnowledgeSeeder seeder;

    @MockBean
    private EmbeddingService embeddingService;

    @Autowired
    private GeminiProperties geminiProperties;

    @BeforeEach
    void setUp() {
        docRepository.deleteAll();
        when(embeddingService.embedText(anyString())).thenReturn(new float[768]);
    }

    @Test
    @DisplayName("Seeder should be idempotent: running multiple times must not duplicate knowledge documents")
    void testSeederIdempotency() {
        // First run
        seeder.seedKnowledgeDocs();
        long count1 = docRepository.count();
        assertThat(count1).isGreaterThanOrEqualTo(10);

        // Second run
        seeder.seedKnowledgeDocs();
        long count2 = docRepository.count();
        assertThat(count2).isEqualTo(count1);
    }
}