-- Foreign keys with deliberate delete semantics
ALTER TABLE portfolios 
    ADD CONSTRAINT fk_portfolios_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE holdings 
    ADD CONSTRAINT fk_holdings_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_holdings_stock FOREIGN KEY (stock_id) REFERENCES stocks(id) ON DELETE RESTRICT;

ALTER TABLE transactions 
    ADD CONSTRAINT fk_transactions_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_transactions_stock FOREIGN KEY (stock_id) REFERENCES stocks(id) ON DELETE RESTRICT;

-- Unique constraints & implicit unique indexes
ALTER TABLE portfolios ADD CONSTRAINT uq_portfolios_user UNIQUE (user_id);
ALTER TABLE stocks ADD CONSTRAINT uq_stocks_symbol UNIQUE (symbol);
ALTER TABLE holdings ADD CONSTRAINT uq_holdings_port_stock UNIQUE (portfolio_id, stock_id);

-- Authoritative case-insensitive unique indexes for users
CREATE UNIQUE INDEX idx_users_username_lower ON users (LOWER(username));
CREATE UNIQUE INDEX idx_users_email_lower ON users (LOWER(email));

-- Check constraints
ALTER TABLE users ADD CONSTRAINT chk_users_username_len CHECK (length(username) >= 3);
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'));
ALTER TABLE portfolios ADD CONSTRAINT chk_portfolios_cash CHECK (cash_balance >= 0.00);
ALTER TABLE stocks ADD CONSTRAINT chk_stocks_price_pos CHECK (current_price > 0.00);
ALTER TABLE stocks ADD CONSTRAINT chk_stocks_prev_pos CHECK (previous_close > 0.00);
ALTER TABLE holdings ADD CONSTRAINT chk_holdings_qty_pos CHECK (quantity > 0);
ALTER TABLE holdings ADD CONSTRAINT chk_holdings_avg_pos CHECK (average_buy_price > 0.00);
ALTER TABLE transactions ADD CONSTRAINT chk_tx_type CHECK (type IN ('BUY', 'SELL'));
ALTER TABLE transactions ADD CONSTRAINT chk_tx_qty_pos CHECK (quantity > 0);
ALTER TABLE transactions ADD CONSTRAINT chk_tx_price_pos CHECK (execution_price > 0.00);
ALTER TABLE transactions ADD CONSTRAINT chk_tx_total_pos CHECK (total_value > 0.00);

-- Non-unique lookup & sorting indexes
CREATE INDEX idx_holdings_portfolio ON holdings (portfolio_id);
CREATE INDEX idx_transactions_port_date ON transactions (portfolio_id, created_at DESC);

-- HNSW vector similarity search index for cosine similarity
CREATE INDEX idx_knowledge_embedding ON financial_knowledge_docs USING hnsw (embedding vector_cosine_ops);