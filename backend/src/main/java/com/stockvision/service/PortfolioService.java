package com.stockvision.service;

import com.stockvision.dto.HoldingResponse;
import com.stockvision.dto.PortfolioSummaryResponse;
import com.stockvision.dto.TransactionResponse;
import com.stockvision.entity.HoldingEntity;
import com.stockvision.entity.PortfolioEntity;
import com.stockvision.entity.TransactionEntity;
import com.stockvision.exception.ResourceNotFoundException;
import com.stockvision.repository.HoldingRepository;
import com.stockvision.repository.PortfolioRepository;
import com.stockvision.repository.TransactionRepository;
import com.stockvision.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PortfolioService {

    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("100000.00");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;

    public PortfolioService(PortfolioRepository portfolioRepository,
                            HoldingRepository holdingRepository,
                            TransactionRepository transactionRepository) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
    }

    public PortfolioSummaryResponse getPortfolioSummary() {
        UUID userId = SecurityUtils.getCurrentUserId();
        PortfolioEntity portfolio = portfolioRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for user: " + userId));

        List<HoldingEntity> holdings = holdingRepository.findByPortfolioIdWithStock(portfolio.getId());

        BigDecimal cashBalance = portfolio.getCashBalance().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal currentHoldingsValue = BigDecimal.ZERO;

        for (HoldingEntity h : holdings) {
            if (h.getQuantity() != null && h.getQuantity() > 0) {
                BigDecimal qty = BigDecimal.valueOf(h.getQuantity());
                BigDecimal cost = qty.multiply(h.getAverageBuyPrice());
                BigDecimal value = qty.multiply(h.getStock().getCurrentPrice());

                totalInvested = totalInvested.add(cost);
                currentHoldingsValue = currentHoldingsValue.add(value);
            }
        }

        totalInvested = totalInvested.setScale(2, RoundingMode.HALF_UP);
        currentHoldingsValue = currentHoldingsValue.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalPortfolioValue = cashBalance.add(currentHoldingsValue).setScale(2, RoundingMode.HALF_UP);
        BigDecimal unrealizedPnL = currentHoldingsValue.subtract(totalInvested).setScale(2, RoundingMode.HALF_UP);

        BigDecimal unrealizedPnLPercent;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            unrealizedPnLPercent = unrealizedPnL
                .divide(totalInvested, 4, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
        } else {
            unrealizedPnLPercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal portfolioReturnPercent = totalPortfolioValue
            .subtract(INITIAL_CAPITAL)
            .divide(INITIAL_CAPITAL, 4, RoundingMode.HALF_UP)
            .multiply(ONE_HUNDRED)
            .setScale(2, RoundingMode.HALF_UP);

        return new PortfolioSummaryResponse(
            cashBalance,
            totalInvested,
            currentHoldingsValue,
            totalPortfolioValue,
            unrealizedPnL,
            unrealizedPnLPercent,
            portfolioReturnPercent
        );
    }

    public List<HoldingResponse> getHoldings() {
        UUID userId = SecurityUtils.getCurrentUserId();
        PortfolioEntity portfolio = portfolioRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for user: " + userId));

        List<HoldingEntity> holdings = holdingRepository.findByPortfolioIdWithStock(portfolio.getId());

        BigDecimal currentHoldingsValue = BigDecimal.ZERO;
        for (HoldingEntity h : holdings) {
            if (h.getQuantity() != null && h.getQuantity() > 0) {
                BigDecimal qty = BigDecimal.valueOf(h.getQuantity());
                BigDecimal value = qty.multiply(h.getStock().getCurrentPrice());
                currentHoldingsValue = currentHoldingsValue.add(value);
            }
        }

        List<HoldingResponse> responses = new ArrayList<>();
        for (HoldingEntity h : holdings) {
            if (h.getQuantity() != null && h.getQuantity() > 0) {
                BigDecimal qty = BigDecimal.valueOf(h.getQuantity());
                BigDecimal avgBuy = h.getAverageBuyPrice().setScale(2, RoundingMode.HALF_UP);
                BigDecimal curPrice = h.getStock().getCurrentPrice().setScale(2, RoundingMode.HALF_UP);
                BigDecimal curVal = qty.multiply(curPrice).setScale(2, RoundingMode.HALF_UP);
                BigDecimal unrealizedPnL = qty.multiply(curPrice.subtract(avgBuy)).setScale(2, RoundingMode.HALF_UP);

                BigDecimal allocationPercent;
                if (currentHoldingsValue.compareTo(BigDecimal.ZERO) > 0) {
                    allocationPercent = curVal
                        .divide(currentHoldingsValue, 4, RoundingMode.HALF_UP)
                        .multiply(ONE_HUNDRED)
                        .setScale(2, RoundingMode.HALF_UP);
                } else {
                    allocationPercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                }

                responses.add(new HoldingResponse(
                    h.getStock().getSymbol(),
                    h.getStock().getCompanyName(),
                    h.getQuantity(),
                    avgBuy,
                    curPrice,
                    curVal,
                    unrealizedPnL,
                    allocationPercent
                ));
            }
        }

        return responses;
    }

    public List<TransactionResponse> getTransactions() {
        UUID userId = SecurityUtils.getCurrentUserId();
        PortfolioEntity portfolio = portfolioRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found for user: " + userId));

        List<TransactionEntity> txs = transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId());

        return txs.stream()
            .limit(50)
            .map(TransactionResponse::fromEntity)
            .toList();
    }
}