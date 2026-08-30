# StockVision AI — System Architecture & Design Specification

## 1. Executive Architecture Summary

StockVision AI is built as a cloud-native, multi-tier financial SaaS platform designed for high concurrency, strict financial data integrity, and grounded generative AI assistance.

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Client Presentation Tier                          │
│                React 18 Single Page Application (TypeScript)                │
│                 Vite 5 Bundler + Tailwind CSS + Lucide Icons                │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ HTTP /api/v1
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Edge & Reverse Proxy Tier                          │
│                            Nginx 1.27 Alpine                                │
│       ├── Port 80: Serves Static Production Assets (SPA Route Fallback)     │
│       └── /api/  : Reverse Proxies to Spring Boot Backend (Port 8080)       │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Internal Network DNS
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Application Services Tier                          │
│                   Spring Boot 3.3.5 / Java 21 LTS (Non-Root)                │
│                                                                             │
│  ┌───────────────────────┐ ┌───────────────────────┐ ┌───────────────────┐  │
│  │   Security & Auth     │ │     Domain Core       │ │   AI Advisor      │  │
│  │ • Stateless JWT       │ │ • Portfolio Valuation │ │ • Gemini 2.5 Flash│  │
│  │ • BCrypt (Cost 12)    │ │ • Atomic Trading      │ │ • RAG Retriever   │  │
│  │ • SecurityContext     │ │ • Pessimistic Locking │ │ • Read-Only Tools │  │
│  └───────────────────────┘ └───────────────────────┘ └───────────────────┘  │
└───────────────────────┬─────────────────────────────────────┬───────────────┘
                        │                                     │
                        ▼                                     ▼
┌──────────────────────────────────────────────┐ ┌────────────────────────────┐
│              Persistence Tier                │ │    External AI Services    │
│            PostgreSQL 16 + pgvector          │ │ Google Generative Language │
│  • Flyway Migrations (V1, V2, V3)            │ │ REST API (v1beta)          │
│  • vector(768) + HNSW Cosine Index (<=>)     │ │ • gemini-2.5-flash         │
│  • Hibernate ddl-auto: validate              │ │ • gemini-embedding-2       │
│  • Named Volume: postgres-data               │ └────────────────────────────┘
└──────────────────────────────────────────────┘
```

---

## 2. Backend Layered Architecture

The backend follows a strict layered separation of concerns:

```text
com.stockvision
├── ai/tools/       # Server-enforced, read-only AI tools with SecurityContext isolation
├── client/         # Direct REST client for Gemini v1beta Generative Language API
├── config/         # SecurityFilterChain, GeminiProperties, and JPA bean definitions
├── controller/     # Thin REST controllers handling HTTP requests and DTO mapping
├── dto/            # Immutable request/response transfer objects
├── entity/         # JPA entities mapped to PostgreSQL relational tables
├── exception/      # GlobalExceptionHandler mapping domain errors to ApiErrorResponse
├── repository/     # Spring Data JPA repositories with pessimistic lock queries
├── security/       # JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetailsService
└── service/        # Transactional business logic (Auth, Market, Portfolio, Trade, AI, RAG)
```

### Layer Responsibilities
1. **Controllers (`@RestController`):** Validate input annotations (`@Valid`), delegate execution to service beans, and return typed `ResponseEntity<T>`. Controllers never execute SQL or business calculations directly.
2. **Services (`@Service`):** Orchestrate transactions (`@Transactional`), enforce business rules, calculate financial metrics with `BigDecimal`, and manage database locks.
3. **Repositories (`@Repository`):** Spring Data JPA interfaces. Includes `@Lock(LockModeType.PESSIMISTIC_WRITE)` queries for portfolio concurrency control and native SQL pgvector cosine similarity queries.
4. **Entities (`@Entity`):** Relational mappings with UUID primary keys and standard JPA audit timestamps.

---

## 3. Frontend Architecture

The frontend is a modular React 18 single-page application built with TypeScript and Tailwind CSS:

```text
src/
├── api/            # Centralized API fetch wrapper and typed domain endpoints
├── components/
│   ├── ai/         # AiChatPanel, StockAnalysisModal
│   ├── common/     # MetricCard, LoadingSpinner, ErrorAlert, Badge
│   ├── layout/     # AppLayout, Navbar, Sidebar
│   ├── market/     # StockTable
│   ├── portfolio/  # HoldingsTable, PortfolioAllocationBar
│   └── trading/    # TradeModal (accessible dialog with focus trapping)
├── contexts/       # AuthContext managing user session and JWT lifecycle
├── hooks/          # Domain hooks (useAuth, usePortfolio, useMarketData, useTrade, useAiAdvisor)
├── pages/          # Full view components (Dashboard, Market, Portfolio, Transactions, AI, Auth)
├── routes/         # ProtectedRoute and PublicRoute route guards
├── types/          # TypeScript interfaces mirroring backend DTOs
└── utils/          # Currency (INR) and percentage formatters
```

---

## 4. Database Schema & Persistence Architecture

The database contains six core tables created and managed strictly by Flyway:

1. **`users`:** Stores credentials (`username`, `email`, `password_hash`, `role`).
2. **`portfolios`:** Stores cash balance with 1:1 user foreign key (`cash_balance NUMERIC(15,2)`).
3. **`stocks`:** Catalogs 10 demo equities with market price and sector metadata.
4. **`holdings`:** Tracks owned shares (`quantity INT`, `average_buy_price NUMERIC(15,2)`).
5. **`transactions`:** Immutable audit log of executed orders (`type VARCHAR(10)`, `quantity INT`, `execution_price NUMERIC(15,2)`, `total_value NUMERIC(15,2)`).
6. **`financial_knowledge_docs`:** Educational corpus with `embedding vector(768)` and HNSW cosine distance index (`financial_docs_embedding_hnsw_idx`).

---

## 5. Security & Isolation Model

* **Stateless Authentication:** Spring Security validates JWT bearer tokens on every request. Tokens carry user ID, username, and role claims signed with HMAC-SHA512.
* **Tenant Isolation:** Tenant identity is resolved authoritatively via `SecurityUtils.getCurrentUserId()`. No user can inspect, modify, or trade another user's portfolio.
* **Price Authority:** Market and trade execution prices are resolved exclusively from the database repository.
* **AI Advisory Boundary:** Gemini models have zero tools for trade execution or database mutations. Prompt injections attempting to force trades are intercepted and safely refused.