CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS portfolios (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    cash_balance NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS stocks (
    id UUID PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    sector VARCHAR(50) NOT NULL,
    current_price NUMERIC(15, 2) NOT NULL,
    previous_close NUMERIC(15, 2) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS holdings (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    stock_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    average_buy_price NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL,
    stock_id UUID NOT NULL,
    type VARCHAR(10) NOT NULL,
    quantity INTEGER NOT NULL,
    execution_price NUMERIC(15, 2) NOT NULL,
    total_value NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS financial_knowledge_docs (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    embedding TEXT,
    source VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);