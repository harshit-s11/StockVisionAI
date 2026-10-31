
# StockVision AI — Intelligent Financial Analytics & Portfolio Platform

StockVision AI is a full-stack portfolio management, market analytics, and simulated trading platform with AI-powered educational insights grounded by Retrieval-Augmented Generation (RAG).

Users can explore simulated Indian equities, manage a demo portfolio, execute virtual trades, track performance, and ask an AI advisor questions using their portfolio metrics and curated financial knowledge.

> [!IMPORTANT]
> **Educational Demo Only:** StockVision AI is a learning and demonstration project. It does not provide financial advice, recommend securities, connect to a brokerage, or execute real trades. Market prices and trading activity are simulated.

---

## Live Demo

- **Web Application:** https://stock-vision-ai-ten.vercel.app
- **Backend Health Check:** https://stockvision-api-vj7s.onrender.com/api/v1/health
- **Source Code:** https://github.com/harshit-s11/StockVisionAI

Each new account starts with **₹100,000 in simulated demo capital**.

> [!NOTE]
> **Free-Tier Cold Starts:** The backend is hosted on Render's free tier and may sleep after inactivity. The first request after a period of inactivity can take some time while the service starts. If the application displays a server-waking message, allow the backend to start and retry.
>
> All displayed market prices and changes are simulated and are not live exchange data.

---

## Key Features

- **Multi-Tenant Portfolio Tracking:** Server-authoritative calculations for total invested value, holdings value, total portfolio value, unrealized P&L, portfolio return, and holding allocation.
- **JWT Authentication:** User registration and login with BCrypt password hashing and stateless JWT-based authentication.
- **Automatic Demo Capital:** New users receive ₹100,000 in simulated starting capital.
- **Atomic Trading Engine:** BUY and SELL operations use transactional database updates and pessimistic locking to protect portfolio balances and prevent concurrent trade inconsistencies.
- **Dynamic Market Equities Catalog:** Searchable and filterable catalog of demo Indian equities with sector classification and scheduled simulated price fluctuations.
- **Weighted Average Cost Basis:** Average purchase cost is recalculated when additional shares are purchased.
- **Transaction History:** Chronological records of simulated trade executions.
- **AI Portfolio Advisor with RAG:** Educational financial chat and stock analysis powered by Google Gemini and pgvector similarity search across curated financial knowledge.
- **Portfolio-Grounded AI Responses:** Advisor answers can use relevant portfolio metrics and retrieved educational content.
- **Readable AI Responses:** Markdown-rendered answers with separate knowledge-source labels.
- **AI Response Reliability:** Handles multi-part Gemini responses, checks generation completion metadata, and uses bounded retry logic for token-limited responses.
- **Read-Only AI Safety Boundary:** The advisor cannot execute trades, deduct cash, or modify portfolio data.
- **Responsive Web UI:** React and TypeScript single-page application styled with Tailwind CSS.

---

## Architecture Overview

### Production Deployment

```text
                         User Browser
                              |
                              v
                   React + Vite Frontend
                         (Vercel)
                              |
                       REST API / JWT
                              |
                              v
                   Spring Boot Backend
                         (Render)
                    /        |        \
                   v         v         v
          PostgreSQL +    Gemini    Spring Security
             pgvector       API         + JWT
              (Neon)
```

### Local Docker Deployment

The repository also includes a Docker Compose configuration for local multi-container development and testing.

```text
                         Browser
                            |
                            v
                          Nginx
                     /             \
                    v               v
             React + Vite SPA   Spring Boot API
                                    |
                                    v
                          PostgreSQL + pgvector
```

---

## AI Advisor Workflow

1. The user submits a question through the AI Advisor interface.
2. The backend loads relevant portfolio context when required.
3. The RAG pipeline retrieves relevant financial knowledge from the pgvector knowledge base.
4. The backend constructs a grounded prompt using portfolio metrics and retrieved content.
5. Google Gemini generates an educational response.
6. The backend extracts text from all response parts and checks Gemini completion metadata.
7. Token-limit responses are handled through a bounded retry rather than silently returning incomplete output.
8. The frontend renders the response as Markdown and displays retrieved knowledge sources separately.

The AI Advisor is educational and read-only. It does not execute trades or modify portfolio state.

---

## Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.3.5, Spring Web, Spring Data JPA, Spring Security |
| **Database** | PostgreSQL, pgvector, Flyway |
| **AI / RAG** | Google Gemini API, pgvector similarity search |
| **Frontend** | React, TypeScript, Vite, Tailwind CSS, React Router |
| **Authentication** | JWT, BCrypt |
| **Testing** | JUnit 5, Mockito, Vitest, React Testing Library |
| **Frontend Hosting** | Vercel |
| **Backend Hosting** | Render |
| **Database Hosting** | Neon |

---

## Project Structure

```text
StockVisionAI/
├── backend/
│   ├── src/main/java/com/stockvision/
│   │   ├── ai/tools/            # Server-enforced read-only AI tools
│   │   ├── client/              # Gemini HTTP REST client
│   │   ├── config/              # Security, Gemini, and JPA configuration
│   │   ├── controller/          # REST API endpoints
│   │   ├── dto/                 # Request and response contracts
│   │   ├── entity/              # JPA domain entities
│   │   ├── exception/            # Global exception handling
│   │   ├── repository/           # Spring Data JPA repositories
│   │   ├── security/             # JWT provider and security filters
│   │   └── service/              # Business, portfolio, trade, AI, and RAG services
│   ├── src/main/resources/
│   │   ├── application.yml       # Externalized Spring configuration
│   │   └── db/migration/         # Flyway SQL migrations
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── api/                  # API client and endpoint functions
│   │   ├── components/           # Reusable UI components
│   │   ├── contexts/             # React context providers
│   │   ├── hooks/                # Custom application hooks
│   │   ├── pages/                # Dashboard, market, portfolio, trades, AI, auth
│   │   ├── routes/               # Public and protected routes
│   │   └── types/                # TypeScript interfaces
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   └── vite.config.ts
├── docs/
│   ├── ARCHITECTURE.md           # Architecture documentation
│   ├── API.md                    # REST API reference
│   ├── MIGRATION_SUMMARY.md      # Legacy migration notes
│   ├── DOCKER.md                 # Docker deployment guide
│   └── DEVELOPMENT.md            # Developer workflow and testing
├── docker-compose.yml
└── .env.example
```

---

## Prerequisites

For local development:

- **Java:** JDK 21
- **Build Tool:** Apache Maven 3.9+
- **Node.js:** Node.js 20+ with npm
- **Database:** PostgreSQL with the pgvector extension
- **Docker (Optional):** Docker Engine and Docker Compose
- **Gemini API Key:** Required for AI Advisor functionality

---

## Environment Variables

Use `.env.example` as a template for local configuration. Do not commit real credentials or secret values.

### Backend Configuration

```env
# PostgreSQL Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/stockvision
SPRING_DATASOURCE_USERNAME=<your_database_username>
SPRING_DATASOURCE_PASSWORD=<your_database_password>

# JWT Authentication
JWT_SECRET=<your_secure_jwt_secret>
JWT_EXPIRATION_MS=86400000

# Google Gemini
GEMINI_API_KEY=<your_gemini_api_key>

# AI Generation
GEMINI_MAX_OUTPUT_TOKENS=1200

# Market Simulation
MARKET_SIMULATION_ENABLED=true
MARKET_SIMULATION_INTERVAL_MS=15000
MARKET_SIMULATION_INITIAL_DELAY_MS=10000
MARKET_SIMULATION_MAX_CHANGE_PERCENT=1.50
MARKET_SIMULATION_MIN_PRICE=1.00
```

The exact environment variable names and defaults are defined in the backend configuration. Refer to `application.yml` and `.env.example` when configuring a local instance.

### Frontend Configuration

Create `frontend/.env` for local development:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

The deployed Vercel frontend uses:

```env
VITE_API_BASE_URL=https://stockvision-api-vj7s.onrender.com/api/v1
```

> [!WARNING]
> Vite variables prefixed with `VITE_` are exposed to the browser. Only place public configuration such as the API base URL in these variables. Never put Gemini keys, JWT secrets, or database credentials in frontend environment variables.

---

## Running Locally

### 1. Start PostgreSQL

Ensure PostgreSQL is running locally with a database named `stockvision` and the pgvector extension available.

Flyway applies the database migrations when the backend starts.

### 2. Start the Backend

From the repository root:

```bash
cd backend
mvn spring-boot:run
```

On Windows PowerShell, if using the Maven wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

### 3. Start the Frontend

Open another terminal:

```bash
cd frontend
npm install
npm run dev
```

The Vite development server runs on:

```text
http://localhost:5173
```

Configure `VITE_API_BASE_URL` to point to the local backend API.

---

## Running with Docker

Validate the Compose configuration:

```bash
docker compose config
```

Build and start the services:

```bash
docker compose up --build -d
```

Check service status:

```bash
docker compose ps
```

View logs:

```bash
docker compose logs -f
```

The local application URL depends on the port mapping configured in `docker-compose.yml`—typically:

```text
http://localhost
```

---

## API Summary

All API endpoints are versioned under `/api/v1`.

| Method | Endpoint | Auth | Purpose |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/v1/health` | Public | Backend health check |
| `POST` | `/api/v1/auth/register` | Public | Register a user and initialize demo capital |
| `POST` | `/api/v1/auth/login` | Public | Authenticate and return a JWT |
| `GET` | `/api/v1/auth/me` | JWT | Retrieve the authenticated user profile |
| `GET` | `/api/v1/stocks` | Public | Retrieve the demo equities catalog |
| `GET` | `/api/v1/stocks/{symbol}` | Public | Retrieve a stock quote |
| `GET` | `/api/v1/portfolio` | JWT | Retrieve portfolio summary and metrics |
| `GET` | `/api/v1/portfolio/holdings` | JWT | Retrieve active holdings |
| `GET` | `/api/v1/transactions` | JWT | Retrieve trade history |
| `POST` | `/api/v1/trades/buy` | JWT | Execute a simulated BUY order |
| `POST` | `/api/v1/trades/sell` | JWT | Execute a simulated SELL order |
| `POST` | `/api/v1/ai/chat` | JWT | Portfolio-grounded AI chat with RAG |
| `POST` | `/api/v1/ai/analyze-stock` | JWT | Educational stock analysis |

See [`docs/API.md`](docs/API.md) for request and response details.

---

## Financial Calculation Principles

Financial metrics are calculated on the backend using `java.math.BigDecimal`, with `RoundingMode.HALF_UP` and two decimal places.

- **Stock Price Change**

  `Change = Current Price - Previous Close`

- **Stock Price Change Percentage**

  `Change % = ((Current Price - Previous Close) / Previous Close) × 100`

- **Total Invested / Cost Basis**

  `Total Invested = Σ (Holding Quantity × Average Buy Price)`

- **Current Holdings Value**

  `Holdings Value = Σ (Holding Quantity × Current Market Price)`

- **Total Portfolio Value**

  `Total Portfolio Value = Cash Balance + Current Holdings Value`

- **Unrealized P&L**

  `Unrealized P&L = Current Holdings Value - Total Invested`

- **Unrealized P&L Percentage**

  `Unrealized P&L % = (Unrealized P&L / Total Invested) × 100`

- **Portfolio Return Percentage**

  `Portfolio Return % = ((Total Portfolio Value - Initial Capital) / Initial Capital) × 100`

- **Holding Allocation Percentage**

  `Allocation % = (Holding Current Value / Total Portfolio Value) × 100`

- **Weighted Average Buy Price**

  `New Average Price = ((Existing Quantity × Existing Average Price) + (Buy Quantity × Execution Price)) / (Existing Quantity + Buy Quantity)`

---

## Security Architecture

1. **Server-Derived Identity:** User identity is resolved server-side from the authenticated security context. Clients cannot select or spoof another user's portfolio.
2. **Server-Resolved Prices:** Trade execution prices are obtained from backend market data rather than trusted client-supplied prices.
3. **Transactional Trade Execution:** BUY and SELL operations use database transactions to maintain consistent cash, holdings, and transaction records.
4. **Pessimistic Locking:** Portfolio updates use pessimistic write locks to reduce race conditions during concurrent trades.
5. **Password Hashing:** Passwords are hashed using BCrypt.
6. **JWT Authentication:** Protected endpoints require a valid JWT.
7. **AI Safety Boundary:** AI functionality is read-only and does not have access to trade execution or portfolio write operations.
8. **Prompt Injection Defense:** System instructions define the advisor's educational scope and prevent user prompts from overriding application rules.

---

## Automated Test Coverage

Latest reported local verification for the AI Advisor improvements:

```text
Backend Test Suite (Maven / JUnit):
- Tests run: 115
- Failures: 0
- Errors: 0
- Skipped: 0

Frontend Test Suite (Vitest / React Testing Library):
- Test Files: 6 passed
- Tests run: 17
- Failures: 0
- Errors: 0

TypeScript:
- tsc --noEmit: 0 errors

Frontend Production Build:
- Vite production build: successful
```

Run the backend tests:

```bash
mvn -f backend/pom.xml test
```

Run the frontend checks:

```bash
npm test --prefix frontend -- --run
npm run typecheck --prefix frontend
npm run build --prefix frontend
```

---

## Migration from Legacy StockVision

StockVision AI modernizes the legacy Flask-based prototype with a Java/Spring Boot backend, a React frontend, PostgreSQL, and an AI-powered educational advisor.

| Component | Legacy Application | StockVision AI |
| :--- | :--- | :--- |
| **Language / Framework** | Python / Flask | Java 21 / Spring Boot |
| **Database** | SQLite | PostgreSQL + pgvector |
| **Schema Management** | Ad hoc schema changes | Flyway migrations |
| **Authentication** | Legacy session-based authentication | JWT + BCrypt |
| **Data Integrity** | SQLite write-lock limitations | Transactional updates and database locking |
| **Numeric Precision** | Floating-point calculations | `BigDecimal` with explicit rounding |
| **AI Integration** | Not available | Gemini + pgvector RAG |
| **Frontend** | Server-rendered templates | React + TypeScript |
| **Deployment** | Python application process | Vercel frontend + Render backend |

---

## Known Limitations

1. **Simulated Market Data:** Prices and market movements are generated by the application and are not live exchange data.
2. **Free-Tier Cold Starts:** The Render backend may sleep after inactivity, delaying the first request.
3. **AI Availability and Latency:** AI features depend on Gemini API availability and may experience variable response times.
4. **AI Output Quality:** Gemini-generated educational content may contain inaccuracies and should not be treated as financial advice.
5. **Demo Trading Only:** The application does not connect to a brokerage or execute real trades.
6. **Browser Token Storage:** The current MVP uses browser-side JWT storage. A production financial application should evaluate secure HTTP-only cookies and additional session protections.

---

## Future Improvements

- Integration with a licensed live market-data provider.
- WebSocket-based market updates.
- Refresh-token rotation and HTTP-only cookie session management.
- Caching for frequently requested market data.
- Advanced technical charting and interactive candlestick visualizations.
- More comprehensive portfolio analytics and risk metrics.
- Expanded AI evaluation and observability.
- Kubernetes deployment manifests and Helm charts.
