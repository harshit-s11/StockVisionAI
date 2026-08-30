# StockVision AI — Developer Setup & Testing Guide

## 1. Prerequisites

* **Java Development Kit:** OpenJDK 21 (Temurin or GraalVM recommended)
* **Build Tool:** Apache Maven 3.9+
* **Node.js:** Node.js 20+ / 22+ with `npm`
* **Database:** PostgreSQL 16 with `pgvector` extension installed on `localhost:5432`

---

## 2. Local Development Workflow

### 2.1 Backend Development

```bash
cd backend

# Run automated test suite
mvn clean test

# Start Spring Boot application in development mode
mvn spring-boot:run
```
The backend listens on `http://localhost:8080`. Flyway will automatically execute database migrations on boot.

### 2.2 Frontend Development

```bash
cd frontend

# Install dependencies
npm install

# Run frontend tests
npm test

# Run strict TypeScript check
npx tsc --noEmit

# Build production bundle
npm run build

# Start Vite hot-reloading dev server
npm run dev
```
The frontend dev server starts on `http://localhost:5173`. Requests to `/api/v1` are automatically proxied to port 8080.

---

## 3. Automated Test Suites

### 3.1 Backend Tests (`backend/`)
```bash
mvn clean test
```
The backend includes 64 comprehensive tests covering:
* `AuthControllerIntegrationTest`: Registration, login, validation, 401/409 errors.
* `TradeControllerIntegrationTest`: Atomic BUY/SELL, cash deduction, holding creation/deletion.
* `TradingConcurrencyIntegrationTest`: Concurrent multi-threaded trade safety under pessimistic write locks.
* `PortfolioControllerIntegrationTest`: Dynamic portfolio valuation and allocation calculations.
* `MarketDataControllerIntegrationTest`: Stock catalog and symbol lookups.
* `AiAdvisorControllerIntegrationTest`: Chat and stock analysis with mock Gemini harnesses.
* `CrossUserAiIsolationIntegrationTest`: Multi-tenant data segregation.
* `AiTradingSafetyBoundaryTest`: Zero database mutation safety verification.
* `EmbeddingServiceTest` & `RagServiceTest`: 768-dim vector validation and pgvector cosine similarity search.
* `RagKnowledgeSeederTest`: Knowledge document idempotent seeding.

### 3.2 Frontend Tests (`frontend/`)
```bash
npm test
```
The frontend includes 7 tests across 5 test suites covering:
* `auth.test.tsx`: Login submission and token persistence.
* `trading.test.tsx`: Trade modal payload verification (`symbol` + `quantity`).
* `market.test.tsx`: Stock catalog table rendering with DEMO badges.
* `portfolio.test.tsx`: Metric cards and holdings table states.
* `ai.test.tsx`: AI chat interaction, presets, and disclaimer rendering.

---

## 4. Git Commit Conventions

All commits follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:
* `feat(...)`: New feature implementations
* `fix(...)`: Bug fixes and corrections
* `refactor(...)`: Code refactoring without behavior modification
* `test(...)`: Adding or updating test suites
* `chore(...)`: Configuration, build, or deployment changes
* `docs(...)`: Documentation updates