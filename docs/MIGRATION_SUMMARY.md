# StockVision AI — Architecture Migration & Modernization Summary

## 1. Context & Motivation

The original StockVision application (`C:\Projects\StockVision`) was built as a lightweight monolithic Python Flask prototype utilizing SQLite. While suitable as an initial proof-of-concept, it exhibited fundamental architectural limitations for a production-grade financial platform:

1. **Single-File SQLite Bottleneck:** File-level write locks caused database concurrency contention under concurrent trading load.
2. **Floating-Point Imprecision:** Native Python `float` calculations introduced binary floating-point rounding errors in cash and P&L balances.
3. **Session Vulnerabilities:** Cookie-based sessions lacked cross-origin API scalability and modern stateless security controls.
4. **Tight Monolithic Coupling:** Server-rendered Jinja HTML templates mixed presentation logic with backend routes.
5. **Absence of Intelligent Analytics:** The prototype lacked generative advisory and semantic knowledge search capabilities.

StockVision AI (`C:\Projects\StockVisionAI`) modernizes every tier of the system into an enterprise-ready architecture.

---

## 2. Dimension-by-Dimension Migration Breakdown

### 2.1 Backend Platform: Python Flask $\rightarrow$ Java 21 / Spring Boot 3.3.5

* **Why:** Financial systems require strong type safety, mature transaction management, dependency injection, and high multi-threaded throughput.
* **What Changed:** Replaced Flask procedural routes with Spring Boot 3.3.5 layered architecture (`Controller` $\rightarrow$ `Service` $\rightarrow$ `Repository`), Bean Validation (`@Valid`), and standardized exception handling.
* **Benefit:** Compile-time type safety, automated dependency wiring, declarative transaction management, and thread pool scaling.

---

### 2.2 Database Tier: SQLite $\rightarrow$ PostgreSQL 16 + pgvector

* **Why:** Multi-user trading requires concurrent write access, row-level locking, and high-dimensional vector representations for semantic AI search.
* **What Changed:** Replaced SQLite single-file storage with PostgreSQL 16 relational database equipped with the `pgvector` extension for `vector(768)` embeddings and HNSW indexing.
* **Benefit:** Full ACID compliance, robust row-level concurrency, and native sub-millisecond vector similarity calculations (`<=>`).

---

### 2.3 Schema Management: Ad-hoc DDL $\rightarrow$ Flyway SQL Migrations

* **Why:** Unversioned schema mutations lead to environment drift, broken deployments, and untracked database modifications.
* **What Changed:** Implemented versioned, immutable Flyway migrations (`V1__init_schema.sql`, `V2__create_knowledge_docs.sql`, `V3__seed_stocks.sql`) with Hibernate configured strictly in `validate` mode.
* **Benefit:** Deterministic, reproducible database bootstrapping across local, Docker, and CI environments with zero schema drift.

---

### 2.4 Security & Identity: Cookie Sessions $\rightarrow$ Stateless JWT + BCrypt

* **Why:** Modern single-page applications require stateless, token-based authentication that supports decoupled client-server deployment.
* **What Changed:** Implemented Spring Security 6 with `JwtAuthenticationFilter`, HMAC-SHA512 token signing, BCrypt (cost factor 12) password hashing, and server-derived identity resolution from `SecurityContextHolder`.
* **Benefit:** Complete immunity to session hijacking via decoupled tokens; multi-tenant security boundary preventing spoofed user parameters.

---

### 2.5 Financial Precision: Binary Floats $\rightarrow$ Java `BigDecimal`

* **Why:** Binary floating-point arithmetic (`0.1 + 0.2 != 0.3`) causes precision loss in monetary balances and trade valuations.
* **What Changed:** All monetary values, prices, cash balances, and financial percentages are calculated using `java.math.BigDecimal` with `RoundingMode.HALF_UP` and exact scale of 2 decimal places.
* **Benefit:** Zero floating-point drift; exact, auditable financial calculations matching banking standards.

---

### 2.6 Concurrency Control: None $\rightarrow$ Pessimistic Write Locking

* **Why:** Concurrent BUY/SELL requests on the same account could result in race conditions, negative cash balances, or overselling shares.
* **What Changed:** Implemented `@Lock(LockModeType.PESSIMISTIC_WRITE)` in `PortfolioRepository` and `@Transactional` execution in `TradingService`.
* **Benefit:** Absolute serial execution guarantee for trade mutations; verified under multi-threaded integration stress tests.

---

### 2.7 AI & Knowledge: None $\rightarrow$ Gemini 2.5 Flash + pgvector RAG Pipeline

* **Why:** Investors benefit from contextual financial education and grounded fundamental analysis of their portfolio positions.
* **What Changed:** Built an advisory subsystem integrating Google Gemini (`v1beta` REST API), 768-dimensional embeddings, 4 server-enforced read-only tools, and pgvector cosine similarity retrieval over curated educational articles.
* **Benefit:** Accurate, personalized financial guidance with source citations and strict safety boundaries preventing unauthorized trades.

---

### 2.8 Frontend: Jinja Templates $\rightarrow$ React 18 + TypeScript + Tailwind CSS

* **Why:** Modern financial analytics platforms demand rich interactive dashboards, instant state updates, responsive layouts, and strict client-side typing.
* **What Changed:** Built a modern single-page application using React 18, TypeScript, Vite, Tailwind CSS, Lucide icons, and React Router 6 with declarative public and protected route guards.
* **Benefit:** Sub-second client navigation, responsive UI, centralized API error handling, and zero full-page browser reloads.

---

### 2.9 Deployment: Standalone Python Script $\rightarrow$ Multi-Stage Docker Compose

* **Why:** Local and cloud deployments require isolated container environments, automated health probes, and reverse proxying.
* **What Changed:** Created multi-stage Dockerfiles for backend (Java 21 JRE, unprivileged non-root user) and frontend (Nginx 1.27 Alpine) orchestrated via Docker Compose v5 with health-based startup ordering.
* **Benefit:** Production-style local development, reproducible container builds, and unified single-origin routing through Nginx.