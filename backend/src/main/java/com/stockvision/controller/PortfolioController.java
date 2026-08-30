package com.stockvision.controller;

import com.stockvision.dto.HoldingResponse;
import com.stockvision.dto.PortfolioSummaryResponse;
import com.stockvision.service.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public ResponseEntity<PortfolioSummaryResponse> getPortfolioSummary() {
        PortfolioSummaryResponse summary = portfolioService.getPortfolioSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/holdings")
    public ResponseEntity<List<HoldingResponse>> getHoldings() {
        List<HoldingResponse> holdings = portfolioService.getHoldings();
        return ResponseEntity.ok(holdings);
    }
}