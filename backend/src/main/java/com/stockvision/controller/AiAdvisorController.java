package com.stockvision.controller;

import com.stockvision.dto.AiChatRequest;
import com.stockvision.dto.AiChatResponse;
import com.stockvision.dto.StockAnalysisRequest;
import com.stockvision.dto.StockAnalysisResponse;
import com.stockvision.service.AiAdvisorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiAdvisorController {

    private final AiAdvisorService aiAdvisorService;

    public AiAdvisorController(AiAdvisorService aiAdvisorService) {
        this.aiAdvisorService = aiAdvisorService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        AiChatResponse response = aiAdvisorService.chat(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analyze-stock")
    public ResponseEntity<StockAnalysisResponse> analyzeStock(@Valid @RequestBody StockAnalysisRequest request) {
        StockAnalysisResponse response = aiAdvisorService.analyzeStock(request);
        return ResponseEntity.ok(response);
    }
}