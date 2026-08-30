package com.stockvision.repository;

import com.stockvision.entity.FinancialKnowledgeDocEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialKnowledgeDocRepository extends JpaRepository<FinancialKnowledgeDocEntity, UUID> {

    @Query(value = "SELECT * FROM financial_knowledge_docs WHERE embedding IS NOT NULL ORDER BY embedding <=> CAST(:queryVector AS vector) LIMIT :topK", nativeQuery = true)
    List<FinancialKnowledgeDocEntity> findSimilarDocuments(@Param("queryVector") String queryVector, @Param("topK") int topK);

    Optional<FinancialKnowledgeDocEntity> findBySourceAndTitleAndChunkIndex(String source, String title, Integer chunkIndex);

    boolean existsBySourceAndTitleAndChunkIndex(String source, String title, Integer chunkIndex);
}