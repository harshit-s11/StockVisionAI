package com.stockvision.ai.tools;

import com.stockvision.dto.HoldingResponse;
import com.stockvision.dto.PortfolioSummaryResponse;
import com.stockvision.dto.StockResponse;
import com.stockvision.dto.TransactionResponse;
import com.stockvision.service.MarketDataService;
import com.stockvision.service.PortfolioService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiServerTools {

    private final PortfolioService portfolioService;
    private final MarketDataService marketDataService;

    public AiServerTools(PortfolioService portfolioService, MarketDataService marketDataService) {
        this.portfolioService = portfolioService;
        this.marketDataService = marketDataService;
    }

    // Tool 1 — Current Portfolio Summary (Strictly derived from SecurityContext)
    public PortfolioSummaryResponse getCurrentPortfolio() {
        return portfolioService.getPortfolioSummary();
    }

    // Tool 2 — Current Holdings (Strictly derived from SecurityContext)
    public List<HoldingResponse> getCurrentHoldings() {
        return portfolioService.getHoldings();
    }

    // Tool 3 — Stock Quote (Public market lookup by ticker symbol)
    public StockResponse getStockQuote(String symbol) {
        return marketDataService.getStockBySymbol(symbol);
    }

    // Tool 4 — Recent Transactions (Strictly derived from SecurityContext)
    public List<TransactionResponse> getRecentTransactions() {
        return portfolioService.getTransactions();
    }
}