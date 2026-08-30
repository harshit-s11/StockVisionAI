package com.stockvision.controller;

import com.stockvision.dto.TransactionResponse;
import com.stockvision.service.PortfolioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final PortfolioService portfolioService;

    public TransactionController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions() {
        List<TransactionResponse> transactions = portfolioService.getTransactions();
        return ResponseEntity.ok(transactions);
    }
}