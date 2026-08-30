package com.stockvision.service;

import com.stockvision.dto.StockResponse;
import com.stockvision.exception.ResourceNotFoundException;
import com.stockvision.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MarketDataService {

    private final StockRepository stockRepository;

    public MarketDataService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public List<StockResponse> getAllStocks() {
        return stockRepository.findAll().stream()
            .map(StockResponse::fromEntity)
            .toList();
    }

    public StockResponse getStockBySymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new ResourceNotFoundException("Stock symbol must not be empty");
        }
        return stockRepository.findBySymbolIgnoreCase(symbol.trim())
            .map(StockResponse::fromEntity)
            .orElseThrow(() -> new ResourceNotFoundException("Stock not found with symbol: " + symbol.trim().toUpperCase()));
    }
}