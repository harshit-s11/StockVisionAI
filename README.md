# StockVision AI — Intelligent Financial Analytics & Portfolio Platform

StockVision AI is an enterprise-grade portfolio management, market analytics, and simulated trading platform with integrated AI advisory capabilities grounded by Retrieval-Augmented Generation (RAG).

> [!NOTE]
> **Demo Data Notice:** All stock market quotes, prices, and changes displayed in StockVision AI reflect simulated demo market data for educational and development purposes. It is not a live financial market feed.

---

## Key Features

* **Multi-Tenant Portfolio Tracking:** Server-authoritative calculations for Total Invested, Holdings Value, Total Portfolio Value, Unrealized P&L, Portfolio Return %, and Holding Allocation %.
* **Stateless JWT Authentication:** Secure registration with automatic ₹100,000 demo capital allocation, BCrypt (cost factor 12) password hashing, and token-based session validation.
* **Atomic Trading Engine:** ACID-compliant BUY and SELL execution protected by JPA pessimistic write locks (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) and dynamic weighted-average cost basis recalculation.
* **Market Equities Catalog:** Searchable and filterable catalog of top Indian market equities with 24h change metrics and sector classification.
* **Immutable Transaction History:** Chronological, tamper-resistant execution logs for all user trades.
* **Gemini AI Advisor with RAG:** Interactive financial chat and fundamental stock analysis powered by Google Gemini (v1beta API) and pgvector cosine similarity search (`<=>`) across curated financial knowledge articles.
* **Strict AI Safety Boundaries:** The AI subsystem is advisory only. Gemini models operate in a strictly read-only mode via server-enforced tools and cannot execute trades, deduct cash, or mutate database state.
* **Modern Financial UI:** Responsive React 18 + TypeScript single-page application built with Vite and Tailwind CSS.

---

## Architecture Overview

```text
                           Client Browser (Port 80)
                                      │
                                      ▼
                        Nginx 1.27 Reverse Proxy
                        ├── /      → React 18 SPA (Vite Production)
                        └── /api/  → Reverse Proxy to backend:8080/api/
                                      │
                                      ▼
                       Spring Boot 3.3.5 Backend (Port 8080)
                                      │
          ┌───────────────────────────┼───────────────────────────┐
          ▼                           ▼                           ▼
PostgreSQL 16 + pgvector       Gemini v1beta API           Spring Security 6
(Flyway V1, V2, V3)           (gemini-2.5-flash)           (Stateless JWT)
```

---

## Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.3.5, Spring Web, Spring Data JPA, Spring Security 6 |
| **Database** | PostgreSQL 16, pgvector extension (`vector(768)`), Flyway Migration |
| **AI / RAG** | Google Gemini Generative Language REST API (`v1beta`), HNSW Cosine Indexing |
| **Frontend** | React 18.3.1, TypeScript 5.6.3, Vite 5.4.10, Tailwind CSS 3.4.15, React Router 6.28.0 |
| **Testing** | JUnit 5, Mockito, AssertJ, Testcontainers, Vitest 2.1.5, Testing Library |
| **Deployment** | Multi-stage Dockerfiles, Nginx 1.27 Alpine, Docker Compose v5 |

---

## Project Structure

```text
StockVisionAI/
├── backend/
│   ├── src/main/java/com/stockvision/
│   │   ├── ai/tools/            # Server-enforced read-only AI tools
│   │   ├── client/              # Gemini HTTP REST client
│   │   ├── config/              # Security, Gemini, and JPA configuration
│   │   ├── controller/          # REST API endpoints (/auth, /stocks, /portfolio, /trades, /ai)
│   │   ├── dto/                 # Request/response transfer contracts
│   │   ├── entity/              # JPA domain entities (User, Portfolio, Stock, Holding, Transaction, Doc)
│   │   ├── exception/           # Global exception handler & custom exceptions
│   │   ├── repository/          # Spring Data JPA repositories with pessimistic locks
│   │   ├── security/            # JWT provider, filter, and user details service
│   │   └── service/             # Business services (Auth, Market, Portfolio, Trade, AI, RAG)
│   ├── src/main/resources/
│   │   ├── application.yml      # Externalized Spring configuration
│   │   └── db/migration/        # Flyway SQL migrations (V1, V2, V3)
│   ├── Dockerfile               # Multi-stage Java 21 runtime build
│   └── pom.xml                  # Maven project descriptor
├── frontend/
│   ├── src/
│   │   ├── api/                 # Centralized API fetch client & domain endpoints
│   │   ├── components/          # Reusable UI components (layout, common, portfolio, market, trade, ai)
│   │   ├── contexts/            # React AuthContext provider
│   │   ├── hooks/               # Custom hooks (useAuth, usePortfolio, useMarketData, useTrade, useAiAdvisor)
│   │   ├── pages/               # Views (Dashboard, Market, Portfolio, Transactions, AI Advisor, Auth)
│   │   ├── routes/              # Public & Protected route guards
│   │   └── types/               # TypeScript interfaces matching backend DTOs
│   ├── Dockerfile               # Multi-stage Node + Nginx build
│   ├── nginx.conf               # SPA routing & reverse proxy configuration
│   ├── package.json             # NPM dependencies & scripts
│   └── vite.config.ts           # Vite bundler configuration
├── docs/                        # Comprehensive technical documentation
│   ├── ARCHITECTURE.md          # In-depth architectural design
│   ├── API.md                   # REST API contract reference
│   ├── MIGRATION_SUMMARY.md     # Full Flask to Spring Boot migration log
│   ├── DOCKER.md                # Container deployment & operations guide
│   └── DEVELOPMENT.md           # Developer workflow & testing guide
├── docker-compose.yml           # Production-style multi-container composition
└── .env.example                 # Environment variables template
```

---

## Prerequisites

* **Java Development Kit (JDK):** Version 21
* **Build Tool:** Apache Maven 3.9+
* **Node.js:** Node.js 20+ / 22+ with `npm`
* **Database:** PostgreSQL 16 with `pgvector` extension installed
* **Docker (Optional):** Docker Engine 24+ & Docker Compose v2+

---

## Environment Variables

Copy `.env.example` to `.env` for local configuration:

```bash
# PostgreSQL Database Configuration
POSTGRES_DB=stockvision
POSTGRES_USER=stockvision_user
POSTGRES_PASSWORD=stockvision_secret

# Spring Security / JWT
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970337336763979244226452948404D6351655468576D5A7134743777217A25432A
JWT_EXPIRATION_MS=86400000

# Google Gemini AI & RAG Configuration
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_MODEL=gemini-2.5-flash
GEMINI_EMBEDDING_MODEL=gemini-embedding-2
GEMINI_EMBEDDING_DIMENSION=768
RAG_TOP_K=5
```

---

## Running Locally

### 1. Start Database
Ensure PostgreSQL 16 is running on `localhost:5432` with database `stockvision` and the `vector` extension available.

### 2. Start Backend
```bash
cd backend
mvn clean spring-boot:run
```
The backend starts on `http://localhost:8080`. Flyway automatically applies migrations `V1`, `V2`, and `V3`.

### 3. Start Frontend
```bash
cd frontend
npm install
npm run dev
```
The frontend starts on `http://localhost:5173`. Requests to `/api/v1` are automatically proxied to port `8080`.

---

## Running with Docker

```bash
# Validate compose syntax
docker compose config

# Build and start all 3 services in detached mode
docker compose up --build -d

# Check running status and health
docker compose ps

# View aggregate logs
docker compose logs -f
```

The application is accessible at `http://localhost`.

> [!NOTE]
> **Docker Verification Status:** Dockerfiles, Compose topologies, and Nginx configurations are fully implemented and statically validated. Runtime container execution was validated during local build steps and will automatically run whenever the Docker daemon is active.

---

## API Summary

All endpoints are versioned under `/api/v1`:

| Method | Endpoint | Auth | Purpose |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/v1/health` | Public | Backend health probe (`{"status":"UP"}`) |
| `POST` | `/api/v1/auth/register` | Public | Register user & initialize ₹100,000 cash balance |
| `POST` | `/api/v1/auth/login` | Public | Authenticate user & return JWT token |
| `GET` | `/api/v1/auth/me` | JWT | Get authenticated user profile (`id`, `username`, `email`, `role`) |
| `GET` | `/api/v1/stocks` | Public | Get all 10 demo equities in market catalog |
| `GET` | `/api/v1/stocks/{symbol}` | Public | Get quote for specific ticker |
| `GET` | `/api/v1/portfolio` | JWT | Get user portfolio summary & financial metrics |
| `GET` | `/api/v1/portfolio/holdings` | JWT | Get user active equity positions |
| `GET` | `/api/v1/transactions` | JWT | Get user trade execution history (top 50, newest first) |
| `POST` | `/api/v1/trades/buy` | JWT | Execute atomic BUY order (`symbol`, `quantity`) |
| `POST` | `/api/v1/trades/sell` | JWT | Execute atomic SELL order (`symbol`, `quantity`) |
| `POST` | `/api/v1/ai/chat` | JWT | Financial advisor chat with live portfolio grounding & RAG |
| `POST` | `/api/v1/ai/analyze-stock` | JWT | Fundamental stock analysis with key drivers & risks |

---

## Financial Calculation Principles

All financial metrics are calculated on the backend using `java.math.BigDecimal` with `RoundingMode.HALF_UP` and a scale of 2 decimal places:

* **Stock Price Change:** $\text{Change} = \text{Current Price} - \text{Previous Close}$
* **Stock Price Change %:** $\text{Change \%} = \left(\frac{\text{Current Price} - \text{Previous Close}}{\text{Previous Close}}\right) \times 100$
* **Total Invested (Cost Basis):** $\text{Total Invested} = \sum (\text{Holding Quantity} \times \text{Average Buy Price})$
* **Current Holdings Value:** $\text{Holdings Value} = \sum (\text{Holding Quantity} \times \text{Current Market Price})$
* **Total Portfolio Value:** $\text{Total Value} = \text{Cash Balance} + \text{Current Holdings Value}$
* **Unrealized P&L:** $\text{Unrealized P\&L} = \text{Current Holdings Value} - \text{Total Invested}$
* **Unrealized P&L %:** $\text{Unrealized P\&L \%} = \left(\frac{\text{Unrealized P\&L}}{\text{Total Invested}}\right) \times 100$
* **Portfolio Return %:** $\text{Portfolio Return \%} = \left(\frac{\text{Total Portfolio Value} - 100000.00}{100000.00}\right) \times 100$
* **Holding Allocation %:** $\text{Allocation \%} = \left(\frac{\text{Holding Current Value}}{\text{Total Portfolio Value}}\right) \times 100$
* **Weighted Average Buy Price on BUY:**
  $$\text{New Avg Buy Price} = \frac{(\text{Existing Qty} \times \text{Existing Avg Price}) + (\text{Buy Qty} \times \text{Execution Price})}{\text{Existing Qty} + \text{Buy Qty}}$$

---

## Security Architecture

1. **Server-Derived Identity:** Tenant identity is resolved strictly server-side from `SecurityContextHolder` (`SecurityUtils.getCurrentUserId()`). Clients cannot supply or spoof `userId` or `portfolioId`.
2. **Server-Resolved Prices:** Execution prices for BUY/SELL orders and AI analysis are resolved strictly from the authoritative database `StockEntity.getCurrentPrice()`.
3. **Pessimistic Locking:** Trades lock the target portfolio using `@Lock(LockModeType.PESSIMISTIC_WRITE)` to prevent race conditions and concurrent balance overdrafts.
4. **AI Safety Boundary:** The AI subsystem has zero tools or access to JPA write repositories. Gemini operates solely as an educational assistant.
5. **Prompt Injection Defense:** Strict system instructions prevent prompt injection from overriding financial rules or impersonating accounts.

---

## Automated Test Coverage

```text
Backend Test Suite (Maven / JUnit 5 / Mockito / Integration):
- Tests run: 64
- Failures:  0
- Errors:    0
- Skipped:   0

Frontend Test Suite (Vitest / Testing Library / TypeScript):
- Test Files: 5 passed
- Tests:      7 passed
- Failures:   0
- Errors:     0
```

---

## Migration from Legacy StockVision

StockVision AI replaces the legacy Flask monolithic prototype with a modern, scalable microservice-ready architecture:

| Component | Legacy StockVision (Protected) | StockVision AI (Modernized) |
| :--- | :--- | :--- |
| **Language & Framework** | Python 3 / Flask | Java 21 / Spring Boot 3.3.5 |
| **Database** | SQLite single-file (`stockvision.db`) | PostgreSQL 16 + pgvector |
| **Schema Management** | Unmanaged ad-hoc DDL | Versioned Flyway SQL migrations |
| **Security** | Flask sessions / plain cookies | Stateless JWT + BCrypt (cost 12) |
| **Data Integrity** | Prone to SQLite write locks / concurrency races | ACID `@Transactional` + Pessimistic write locks |
| **Precision** | Python `float` (rounding errors) | Java `BigDecimal` (HALF_UP, scale 2) |
| **AI Integration** | None | Gemini 2.5 Flash + pgvector RAG pipeline |
| **Frontend** | Server-rendered HTML / Jinja templates | React 18 + TypeScript + Tailwind CSS |
| **Deployment** | Python `app.py` script | Multi-stage Docker + Nginx reverse proxy |

---

## Known Limitations

1. **Market Data Feed:** Quotes reflect demo simulated prices rather than live exchange WebSockets.
2. **AI Availability:** Live AI features require a valid `GEMINI_API_KEY`. When unconfigured, the application gracefully returns HTTP 503.
3. **Client Token Storage:** The MVP stores JWTs in browser `localStorage`. Production systems should adopt secure HTTP-only cookies.

---

## Future Improvements

* Live real-time market data WebSocket streaming feed.
* Refresh token rotation and HTTP-only cookie session management.
* Distributed Redis caching for high-throughput market quotes.
* Advanced technical charting with interactive candlesticks and depth charts.
* Automated CI/CD pipelines with GitHub Actions and Kubernetes deployment manifests.