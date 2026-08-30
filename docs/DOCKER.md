# StockVision AI — Docker Deployment & Operations Guide

## 1. Overview

StockVision AI provides a production-grade multi-container deployment architecture managed via Docker Compose.

```text
┌─────────────────────────────────────────────────────────────┐
│                 Client Browser (Port 80)                    │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│               frontend (Nginx 1.27 Alpine)                  │
│   ├── /       → Static SPA (React 18 + Vite Production)     │
│   └── /api/   → Reverse Proxy to backend:8080/api/          │
└──────────────────────────────┬──────────────────────────────┘
                               │ (stockvision-network)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│          backend (Spring Boot 3.3.5 / Java 21 JRE)          │
│   └── REST APIs, JWT Security, Gemini RAG Advisor           │
└──────────────────────────────┬──────────────────────────────┘
                               │ (stockvision-network)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│        postgres (PostgreSQL 16 + pgvector Extension)        │
│   └── Flyway Migrations (V1, V2, V3) + vector(768)          │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Container Specifications

### 2.1 `postgres`
* **Base Image:** `pgvector/pgvector:pg16`
* **Container Name:** `stockvision-postgres`
* **Internal Network:** `stockvision-network`
* **Volume:** `postgres-data:/var/lib/postgresql/data`
* **Health Check:** `pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}` (interval: 5s, timeout: 5s, retries: 5)

### 2.2 `backend`
* **Base Image:** `eclipse-temurin:21-jre-alpine` (multi-stage from `maven:3.9.9-eclipse-temurin-21-alpine`)
* **Container Name:** `stockvision-backend`
* **User:** `stockvision:stockvision` (unprivileged non-root user)
* **Port Mapping:** `8080:8080`
* **Health Check:** `wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/health` (interval: 10s, timeout: 5s, retries: 5)
* **Startup Dependency:** Waits for `postgres` to be healthy (`condition: service_healthy`).

### 2.3 `frontend`
* **Base Image:** `nginx:1.27-alpine` (multi-stage from `node:22-alpine`)
* **Container Name:** `stockvision-frontend`
* **Port Mapping:** `80:80`
* **Health Check:** `wget --no-verbose --tries=1 --spider http://localhost:80/` (interval: 10s, timeout: 5s, retries: 3)
* **Startup Dependency:** Waits for `backend` to be healthy (`condition: service_healthy`).

---

## 3. Operational Commands

### 3.1 Validate Configuration
```bash
docker compose config
```

### 3.2 Build and Start Stack
```bash
docker compose up --build -d
```

### 3.3 Check Status & Health
```bash
docker compose ps
```

### 3.4 View Aggregate Logs
```bash
docker compose logs -f
```

### 3.5 Stop Stack (Preserving Data Volume)
```bash
docker compose down
```

### 3.6 Clean Rebuild (Destroying Volume)
```bash
docker compose down -v
docker compose build --no-cache
docker compose up -d
```

---

## 4. Verification Status

* **Static Configuration:** Statically validated via `docker compose config`.
* **Multi-Stage Builds:** Dockerfiles, `.dockerignore` filters, and Nginx reverse proxy configurations are fully implemented.
* **Runtime Verification:** Statically verified against local build tools; full container runtime execution will automatically run whenever Docker Desktop / Docker daemon is active on the host machine.