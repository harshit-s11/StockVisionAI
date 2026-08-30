package com.stockvision.service;

import com.stockvision.dto.TradeRequest;
import com.stockvision.dto.TradeResponse;
import com.stockvision.entity.*;
import com.stockvision.exception.InsufficientCashException;
import com.stockvision.exception.InsufficientHoldingsException;
import com.stockvision.exception.ResourceNotFoundException;
import com.stockvision.repository.HoldingRepository;
import com.stockvision.repository.PortfolioRepository;
import com.stockvision.repository.StockRepository;
import com.stockvision.repository.TransactionRepository;
import com.stockvision.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
public class TradingService {

    private final PortfolioRepository portfolioRepository;
    private final StockRepository stockRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;

    public TradingService(PortfolioRepository portfolioRepository,
                          StockRepository stockRepository,
                          HoldingRepository holdingRepository,
                          TransactionRepository transactionRepository) {
        this.portfolioRepository = portfolioRepository;
        this.stockRepository = stockRepository;
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TradeResponse executeBuy(TradeRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        
        // 1. Pessimistic Write Lock on user's portfolio
        PortfolioEntity portfolio = portfolioRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for user: " + userId));

        // 2. Resolve Stock by Symbol
        String symbol = request.getSymbol().trim().toUpperCase();
        StockEntity stock = stockRepository.findBySymbolIgnoreCase(symbol)
            .orElseThrow(() -> new ResourceNotFoundException("Stock not found with symbol: " + symbol));

        // 3. Server-side price resolution
        BigDecimal executionPrice = stock.getCurrentPrice().setScale(2, RoundingMode.HALF_UP);
        int buyQty = request.getQuantity();
        BigDecimal totalValue = executionPrice.multiply(BigDecimal.valueOf(buyQty)).setScale(2, RoundingMode.HALF_UP);

        // 4. Validate cash balance
        if (portfolio.getCashBalance().compareTo(totalValue) < 0) {
            throw new InsufficientCashException("Insufficient cash balance for BUY trade. Required: ₹" + totalValue + ", Available: ₹" + portfolio.getCashBalance());
        }

        // 5. Deduct cash
        BigDecimal newCash = portfolio.getCashBalance().subtract(totalValue).setScale(2, RoundingMode.HALF_UP);
        portfolio.setCashBalance(newCash);
        portfolioRepository.save(portfolio);

        // 6. Create or update holding with weighted average buy price
        Optional<HoldingEntity> optHolding = holdingRepository.findByPortfolioIdAndStockId(portfolio.getId(), stock.getId());
        int holdingQty;
        BigDecimal avgBuyPrice;

        if (optHolding.isEmpty()) {
            HoldingEntity newHolding = new HoldingEntity(portfolio, stock, buyQty, executionPrice);
            holdingRepository.save(newHolding);
            holdingQty = buyQty;
            avgBuyPrice = executionPrice;
        } else {
            HoldingEntity existingHolding = optHolding.get();
            int oldQty = existingHolding.getQuantity();
            BigDecimal oldAvg = existingHolding.getAverageBuyPrice();
            int newTotalQty = oldQty + buyQty;

            BigDecimal totalCost = oldAvg.multiply(BigDecimal.valueOf(oldQty))
                .add(executionPrice.multiply(BigDecimal.valueOf(buyQty)));
            BigDecimal newAvg = totalCost.divide(BigDecimal.valueOf(newTotalQty), 2, RoundingMode.HALF_UP);

            existingHolding.setQuantity(newTotalQty);
            existingHolding.setAverageBuyPrice(newAvg);
            holdingRepository.save(existingHolding);
            holdingQty = newTotalQty;
            avgBuyPrice = newAvg;
        }

        // 7. Create BUY Transaction
        TransactionEntity tx = new TransactionEntity(portfolio, stock, TransactionType.BUY, buyQty, executionPrice, totalValue);
        tx = transactionRepository.save(tx);

        return new TradeResponse(
            tx.getId(),
            symbol,
            "BUY",
            buyQty,
            executionPrice,
            totalValue,
            newCash,
            holdingQty,
            avgBuyPrice,
            tx.getCreatedAt()
        );
    }

    @Transactional
    public TradeResponse executeSell(TradeRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();

        // 1. Pessimistic Write Lock on user's portfolio
        PortfolioEntity portfolio = portfolioRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for user: " + userId));

        // 2. Resolve Stock by Symbol
        String symbol = request.getSymbol().trim().toUpperCase();
        StockEntity stock = stockRepository.findBySymbolIgnoreCase(symbol)
            .orElseThrow(() -> new ResourceNotFoundException("Stock not found with symbol: " + symbol));

        // 3. Resolve Holding
        HoldingEntity holding = holdingRepository.findByPortfolioIdAndStockId(portfolio.getId(), stock.getId())
            .orElseThrow(() -> new InsufficientHoldingsException("Insufficient holdings: you do not own any shares of " + symbol));

        int sellQty = request.getQuantity();
        if (holding.getQuantity() < sellQty) {
            throw new InsufficientHoldingsException("Insufficient holdings: requested " + sellQty + " shares but only own " + holding.getQuantity());
        }

        // 4. Server-side price resolution
        BigDecimal executionPrice = stock.getCurrentPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalValue = executionPrice.multiply(BigDecimal.valueOf(sellQty)).setScale(2, RoundingMode.HALF_UP);

        // 5. Credit cash
        BigDecimal newCash = portfolio.getCashBalance().add(totalValue).setScale(2, RoundingMode.HALF_UP);
        portfolio.setCashBalance(newCash);
        portfolioRepository.save(portfolio);

        // 6. Update or delete holding
        int holdingQty;
        BigDecimal avgBuyPrice;

        if (sellQty == holding.getQuantity()) {
            holdingRepository.delete(holding);
            holdingQty = 0;
            avgBuyPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            int remainingQty = holding.getQuantity() - sellQty;
            holding.setQuantity(remainingQty);
            // averageBuyPrice remains unchanged for partial sells
            holdingRepository.save(holding);
            holdingQty = remainingQty;
            avgBuyPrice = holding.getAverageBuyPrice();
        }

        // 7. Create SELL Transaction
        TransactionEntity tx = new TransactionEntity(portfolio, stock, TransactionType.SELL, sellQty, executionPrice, totalValue);
        tx = transactionRepository.save(tx);

        return new TradeResponse(
            tx.getId(),
            symbol,
            "SELL",
            sellQty,
            executionPrice,
            totalValue,
            newCash,
            holdingQty,
            avgBuyPrice,
            tx.getCreatedAt()
        );
    }
}