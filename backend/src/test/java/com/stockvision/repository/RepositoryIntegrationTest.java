package com.stockvision.repository;

import com.stockvision.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private HoldingRepository holdingRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FinancialKnowledgeDocRepository docRepository;

    @Test
    @DisplayName("Should persist and retrieve User and associated Portfolio")
    void testUserAndPortfolioPersistence() {
        UserEntity user = new UserEntity("alex_trader", "alex@example.com", "hashed_password", Role.ROLE_USER);
        UserEntity savedUser = userRepository.save(user);

        PortfolioEntity portfolio = new PortfolioEntity(savedUser, new BigDecimal("100000.00"));
        PortfolioEntity savedPortfolio = portfolioRepository.save(portfolio);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedPortfolio.getId()).isNotNull();
        assertThat(savedPortfolio.getUser().getId()).isEqualTo(savedUser.getId());

        Optional<UserEntity> fetchedUser = userRepository.findByUsernameIgnoreCase("ALEX_TRADER");
        assertThat(fetchedUser).isPresent();
        assertThat(fetchedUser.get().getEmail()).isEqualTo("alex@example.com");

        Optional<PortfolioEntity> fetchedPortfolio = portfolioRepository.findByUserId(savedUser.getId());
        assertThat(fetchedPortfolio).isPresent();
        assertThat(fetchedPortfolio.get().getCashBalance()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    @DisplayName("Should persist Stock, Holding, and Transaction with accurate relationships")
    void testStockHoldingAndTransactionPersistence() {
        UserEntity user = userRepository.save(new UserEntity("trader_bob", "bob@example.com", "pass123", Role.ROLE_USER));
        PortfolioEntity portfolio = portfolioRepository.save(new PortfolioEntity(user, new BigDecimal("75000.00")));

        StockEntity stock = stockRepository.save(new StockEntity("TCS", "Tata Consultancy Services Ltd.", "Information Technology",
                new BigDecimal("3850.00"), new BigDecimal("3810.00")));

        HoldingEntity holding = holdingRepository.save(new HoldingEntity(portfolio, stock, 10, new BigDecimal("3850.00")));
        TransactionEntity tx = transactionRepository.save(new TransactionEntity(portfolio, stock, TransactionType.BUY, 10,
                new BigDecimal("3850.00"), new BigDecimal("38500.00")));

        assertThat(stock.getId()).isNotNull();
        assertThat(holding.getId()).isNotNull();
        assertThat(tx.getId()).isNotNull();

        List<HoldingEntity> userHoldings = holdingRepository.findByPortfolioId(portfolio.getId());
        assertThat(userHoldings).hasSize(1);
        assertThat(userHoldings.get(0).getStock().getSymbol()).isEqualTo("TCS");
        assertThat(userHoldings.get(0).getQuantity()).isEqualTo(10);

        List<TransactionEntity> userTxs = transactionRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolio.getId());
        assertThat(userTxs).hasSize(1);
        assertThat(userTxs.get(0).getType()).isEqualTo(TransactionType.BUY);
        assertThat(userTxs.get(0).getTotalValue()).isEqualByComparingTo(new BigDecimal("38500.00"));
    }

    @Test
    @DisplayName("Should persist FinancialKnowledgeDoc with vector conversion")
    void testFinancialKnowledgeDocPersistence() {
        float[] sampleEmbedding = new float[]{0.123f, -0.456f, 0.789f};
        FinancialKnowledgeDocEntity doc = new FinancialKnowledgeDocEntity(
                "Diversification",
                "Portfolio diversification reduces unsystematic risk.",
                1,
                "curated_knowledge",
                sampleEmbedding
        );

        FinancialKnowledgeDocEntity savedDoc = docRepository.save(doc);
        assertThat(savedDoc.getId()).isNotNull();

        Optional<FinancialKnowledgeDocEntity> fetchedDoc = docRepository.findById(savedDoc.getId());
        assertThat(fetchedDoc).isPresent();
        assertThat(fetchedDoc.get().getTitle()).isEqualTo("Diversification");
        assertThat(fetchedDoc.get().getEmbedding()).containsExactly(sampleEmbedding);
    }
}