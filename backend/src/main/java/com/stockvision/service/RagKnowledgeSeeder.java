package com.stockvision.service;

import com.stockvision.config.GeminiProperties;
import com.stockvision.entity.FinancialKnowledgeDocEntity;
import com.stockvision.repository.FinancialKnowledgeDocRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class RagKnowledgeSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RagKnowledgeSeeder.class);

    private final FinancialKnowledgeDocRepository docRepository;
    private final EmbeddingService embeddingService;
    private final GeminiProperties geminiProperties;

    public RagKnowledgeSeeder(FinancialKnowledgeDocRepository docRepository,
                              EmbeddingService embeddingService,
                              GeminiProperties geminiProperties) {
        this.docRepository = docRepository;
        this.embeddingService = embeddingService;
        this.geminiProperties = geminiProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedKnowledgeDocs();
        } catch (Exception ex) {
            log.error("Failed to seed knowledge documents during startup: {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    public void seedKnowledgeDocs() {
        List<KnowledgeSeedItem> items = List.of(
            new KnowledgeSeedItem(
                "Educational Guide",
                "Portfolio Diversification & Asset Allocation",
                0,
                "Diversification is a risk management strategy that mixes a wide variety of investments within a portfolio. A well-diversified portfolio contains a mix of distinct asset types and investment vehicles across sectors (such as Information Technology, Banking, Energy, Consumer Goods) in order to limit exposure to any single asset or industry risk."
            ),
            new KnowledgeSeedItem(
                "Educational Guide",
                "Price-to-Earnings (P/E) Ratio Valuation",
                0,
                "The Price-to-Earnings (P/E) ratio is a key valuation metric measuring a company's current share price relative to its per-share earnings. A high P/E ratio could mean that a stock's price is high relative to earnings and possibly overvalued, or that investors are expecting high growth rates in the future."
            ),
            new KnowledgeSeedItem(
                "Educational Guide",
                "Price-to-Book (P/B) Ratio Fundamentals",
                0,
                "The Price-to-Book (P/B) ratio compares a firm's market capitalization to its book value. It is calculated by dividing the company's stock price per share by its book value per share. It is especially useful in evaluating capital-intensive businesses such as banks, financials, and manufacturing companies."
            ),
            new KnowledgeSeedItem(
                "Educational Guide",
                "Risk Management & Position Sizing",
                0,
                "Position sizing refers to the number of shares an investor purchases within their portfolio. Effective risk management dictates that an investor should never risk more than a small predetermined percentage (typically 1% to 5%) of their total portfolio capital on any single security, thereby mitigating catastrophic loss."
            ),
            new KnowledgeSeedItem(
                "Educational Guide",
                "Market Volatility & Stock Beta",
                0,
                "Beta is a measure of a stock's volatility in relation to the overall market. A beta of 1.0 indicates that the stock moves in tandem with the market index. A beta greater than 1.0 suggests higher volatility (growth stocks), while a beta below 1.0 indicates lower volatility (defensive stocks such as FMCG and Utilities)."
            ),
            new KnowledgeSeedItem(
                "Educational Guide",
                "Long-term Value Investing vs Momentum",
                0,
                "Value investing involves buying securities that appear underpriced by fundamental analysis, with an emphasis on dividend yield, stable earnings, and margin of safety. Momentum trading aims to capitalize on the continuance of existing market trends by buying high-performing equities."
            ),
            new KnowledgeSeedItem(
                "Educational Guide",
                "Technical Indicators: Moving Averages (SMA & EMA)",
                0,
                "A Simple Moving Average (SMA) calculates the average price of a stock over a specific period, smoothing out price fluctuations to identify trend direction. Exponential Moving Averages (EMA) give more weight to recent prices, reacting faster to sudden market developments."
            ),
            new KnowledgeSeedItem(
                "Educational Guide",
                "Relative Strength Index (RSI) Momentum Indicator",
                0,
                "The Relative Strength Index (RSI) is a momentum oscillator measuring the speed and change of price movements on a scale from 0 to 100. Traditionally, an RSI reading above 70 indicates an overbought condition, while an RSI below 30 indicates an oversold condition."
            ),
            new KnowledgeSeedItem(
                "Educational Guide",
                "Portfolio Drawdowns and Stop-Loss Principles",
                0,
                "A maximum drawdown measures the peak-to-trough decline during a specific period for an investment. Implementing disciplined stop-loss thresholds helps investors preserve capital and control behavioral biases during market sell-offs."
            ),
            new KnowledgeSeedItem(
                "Educational Guide",
                "Cash Reserves and Liquidity Management",
                0,
                "Holding adequate uninvested cash reserves in a portfolio provides liquidity to capitalize on market corrections and unexpected buying opportunities, while stabilizing overall portfolio volatility during downturns."
            )
        );

        for (KnowledgeSeedItem item : items) {
            Optional<FinancialKnowledgeDocEntity> existingOpt = docRepository.findBySourceAndTitleAndChunkIndex(
                item.source(), item.title(), item.chunkIndex()
            );

            if (existingOpt.isEmpty()) {
                float[] embedding = null;
                if (geminiProperties.isConfigured()) {
                    try {
                        embedding = embeddingService.embedText(item.content());
                    } catch (Exception ex) {
                        log.warn("Could not generate embedding for seed document '{}': {}", item.title(), ex.getMessage());
                    }
                }
                FinancialKnowledgeDocEntity entity = new FinancialKnowledgeDocEntity(
                    item.title(),
                    item.content(),
                    item.chunkIndex(),
                    item.source(),
                    embedding
                );
                docRepository.save(entity);
            } else if (existingOpt.get().getEmbedding() == null && geminiProperties.isConfigured()) {
                try {
                    float[] embedding = embeddingService.embedText(item.content());
                    FinancialKnowledgeDocEntity entity = existingOpt.get();
                    entity.setEmbedding(embedding);
                    docRepository.save(entity);
                } catch (Exception ex) {
                    log.warn("Could not backfill embedding for seed document '{}': {}", item.title(), ex.getMessage());
                }
            }
        }
    }

    private record KnowledgeSeedItem(String source, String title, Integer chunkIndex, String content) {}
}