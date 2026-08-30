# StockVision AI — REST API Specification & Contract Reference

Base URL: `/api/v1`

---

## 1. Health Endpoints

### `GET /api/v1/health`
* **Access:** Public
* **Description:** Health check probe.
* **Response (200 OK):**
```json
{
  "status": "UP",
  "timestamp": "2026-08-30T22:00:00.000Z"
}
```

---

## 2. Authentication Endpoints

### `POST /api/v1/auth/register`
* **Access:** Public
* **Description:** Registers a new user account and automatically allocates ₹100,000 in demo cash reserves.
* **Request Body:**
```json
{
  "username": "sarah_connor",
  "email": "sarah@example.com",
  "password": "password123"
}
```
* **Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": "c1f7b03b-1b15-4e3f-912b-313d33f20f01",
    "username": "sarah_connor",
    "email": "sarah@example.com",
    "role": "ROLE_USER"
  }
}
```

### `POST /api/v1/auth/login`
* **Access:** Public
* **Description:** Authenticates credentials and returns a signed JWT token.
* **Request Body:**
```json
{
  "username": "sarah_connor",
  "password": "password123"
}
```
* **Response (200 OK):** Same schema as `POST /auth/register`.

### `GET /api/v1/auth/me`
* **Access:** Authenticated (`Bearer <token>`)
* **Description:** Returns the authenticated user's profile.
* **Response (200 OK):**
```json
{
  "id": "c1f7b03b-1b15-4e3f-912b-313d33f20f01",
  "username": "sarah_connor",
  "email": "sarah@example.com",
  "role": "ROLE_USER"
}
```

---

## 3. Market Data Endpoints

### `GET /api/v1/stocks`
* **Access:** Public
* **Description:** Retrieves the catalog of all 10 demo equities.
* **Response (200 OK):**
```json
[
  {
    "symbol": "TCS",
    "companyName": "Tata Consultancy Services Ltd.",
    "sector": "Information Technology",
    "currentPrice": 3850.00,
    "previousClose": 3810.00,
    "change": 40.00,
    "changePercent": 1.05,
    "dataStatus": "DEMO",
    "updatedAt": "2026-08-30T22:00:00.000Z"
  }
]
```

### `GET /api/v1/stocks/{symbol}`
* **Access:** Public
* **Description:** Retrieves quote details for a specific stock ticker.
* **Response (200 OK):** Single `StockResponse` object.

---

## 4. Portfolio & Holdings Endpoints

### `GET /api/v1/portfolio`
* **Access:** Authenticated (`Bearer <token>`)
* **Description:** Retrieves financial valuation summary for the authenticated user's portfolio.
* **Response (200 OK):**
```json
{
  "cashBalance": 80750.00,
  "totalInvested": 19250.00,
  "currentHoldingsValue": 19250.00,
  "totalPortfolioValue": 100000.00,
  "unrealizedPnL": 0.00,
  "unrealizedPnLPercent": 0.00,
  "portfolioReturnPercent": 0.00
}
```

### `GET /api/v1/portfolio/holdings`
* **Access:** Authenticated (`Bearer <token>`)
* **Description:** Retrieves all active equity holdings for the authenticated user.
* **Response (200 OK):**
```json
[
  {
    "symbol": "TCS",
    "companyName": "Tata Consultancy Services Ltd.",
    "quantity": 5,
    "averageBuyPrice": 3850.00,
    "currentPrice": 3850.00,
    "currentValue": 19250.00,
    "unrealizedPnL": 0.00,
    "allocationPercent": 19.25
  }
]
```

---

## 5. Transactions & Trading Endpoints

### `GET /api/v1/transactions`
* **Access:** Authenticated (`Bearer <token>`)
* **Description:** Retrieves immutable trade execution history (top 50, newest first).
* **Response (200 OK):**
```json
[
  {
    "id": "e2a4f08e-5b12-4c91-9e23-883a44d01b12",
    "symbol": "TCS",
    "type": "BUY",
    "quantity": 5,
    "executionPrice": 3850.00,
    "totalValue": 19250.00,
    "timestamp": "2026-08-30T22:15:00.000Z"
  }
]
```

### `POST /api/v1/trades/buy`
* **Access:** Authenticated (`Bearer <token>`)
* **Description:** Executes an atomic BUY order. Recalculates weighted average buy price and deducts cash.
* **Request Body:**
```json
{
  "symbol": "TCS",
  "quantity": 5
}
```
* **Response (200 OK):**
```json
{
  "transactionId": "e2a4f08e-5b12-4c91-9e23-883a44d01b12",
  "symbol": "TCS",
  "type": "BUY",
  "quantity": 5,
  "executionPrice": 3850.00,
  "totalValue": 19250.00,
  "cashBalance": 80750.00,
  "holdingQuantity": 5,
  "averageBuyPrice": 3850.00,
  "timestamp": "2026-08-30T22:15:00.000Z"
}
```

### `POST /api/v1/trades/sell`
* **Access:** Authenticated (`Bearer <token>`)
* **Description:** Executes an atomic SELL order. Reduces quantity, credits cash, and removes holding when quantity reaches zero.
* **Request Body:**
```json
{
  "symbol": "TCS",
  "quantity": 5
}
```
* **Response (200 OK):** Same schema as `POST /trades/buy`.

---

## 6. AI Advisor & RAG Endpoints

### `POST /api/v1/ai/chat`
* **Access:** Authenticated (`Bearer <token>`)
* **Description:** Generates grounded financial advice using the user's live portfolio metrics and pgvector knowledge base.
* **Request Body:**
```json
{
  "message": "How diversified is my portfolio?"
}
```
* **Response (200 OK):**
```json
{
  "answer": "Your portfolio holds ₹80,750 in cash (80.75%) and ₹19,250 in TCS (19.25%)...",
  "disclaimer": "Educational information only. Not financial advice. Market data reflects demo prices.",
  "sources": [
    {
      "title": "Portfolio Diversification & Asset Allocation",
      "source": "Educational Guide"
    }
  ]
}
```

### `POST /api/v1/ai/analyze-stock`
* **Access:** Authenticated (`Bearer <token>`)
* **Description:** Generates fundamental analysis, key drivers, and risk factors for a specific equity.
* **Request Body:**
```json
{
  "symbol": "TCS"
}
```
* **Response (200 OK):**
```json
{
  "symbol": "TCS",
  "analysis": "Tata Consultancy Services is an IT leader...",
  "keyFactors": [
    "Sector: Information Technology",
    "Current Price: ₹3850.00 (24h Change: 1.05%)"
  ],
  "risks": [
    "Market volatility in Information Technology sector"
  ],
  "disclaimer": "Educational information only. Not financial advice.",
  "sources": [
    {
      "title": "Price-to-Earnings (P/E) Ratio Valuation",
      "source": "Educational Guide"
    }
  ]
}
```

---

## 7. Standardized Error Contract

All error responses adhere to the typed `ApiErrorResponse` structure:

```json
{
  "timestamp": "2026-08-30T22:20:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient cash balance to complete BUY order",
  "path": "/api/v1/trades/buy",
  "validationErrors": {
    "quantity": "Quantity must be greater than 0"
  }
}
```

| HTTP Status | Error Type | Typical Cause |
| :--- | :--- | :--- |
| **`400`** | `Bad Request` / `Business Error` | Validation failure, insufficient cash, or insufficient shares |
| **`401`** | `Unauthorized` | Missing, expired, or invalid JWT token |
| **`404`** | `Not Found` | Stock ticker or entity not found |
| **`409`** | `Conflict` | Duplicate username or email during registration |
| **`502`** | `Bad Gateway` | Malformed or empty response from Gemini API |
| **`503`** | `Service Unavailable` | Gemini API key unconfigured or external service unavailable |
| **`500`** | `Internal Server Error` | Unexpected server exception |