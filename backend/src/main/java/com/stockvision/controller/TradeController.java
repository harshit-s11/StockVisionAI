package com.stockvision.controller;

import com.stockvision.dto.TradeRequest;
import com.stockvision.dto.TradeResponse;
import com.stockvision.service.TradingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {

    private final TradingService tradingService;

    public TradeController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @PostMapping("/buy")
    public ResponseEntity<TradeResponse> buy(@Valid @RequestBody TradeRequest request) {
        TradeResponse response = tradingService.executeBuy(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sell")
    public ResponseEntity<TradeResponse> sell(@Valid @RequestBody TradeRequest request) {
        TradeResponse response = tradingService.executeSell(request);
        return ResponseEntity.ok(response);
    }
}