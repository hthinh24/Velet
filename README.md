# Velet — Payment & Wallet Core Infrastructure
> A payment & wallet infrastructure focused on consistency-heavy distributed systems patterns. Current scope: Identity, Wallet, and Payment core services — built with an eye toward scaling into a broader super-app-style platform later.

---

## Overview
Velet models the core money-movement layer of a super-app style platform: user identity, wallet balance management, payment reservation/confirmation, and transfer between users and merchants. The focus is on **correctness under concurrency** — safe balance updates, idempotent retries, and consistent state across services under load — rather than breadth of features.

---

## Architecture
![System Architecture](/assets/architecture.png)
*Velet — Payment & Wallet core infrastructure system design*

**Core patterns:**
- Spring Cloud Gateway as single entry point, routing to Identity, Payment, and Wallet
- Saga choreography (Reserve–Confirm–Release), no central orchestrator
- Transactional Outbox + RabbitMQ for reliable async event publishing
- `SELECT FOR UPDATE SKIP LOCKED` for safe concurrent access under multi-instance workers
- Ledger-as-source-of-truth balance model, with Redis as a read-through cache
- Each service owns its own Postgres + Redis; Payment calls Wallet synchronously over HTTP
- Full observability (tracing, metrics) across all three services via OpenTelemetry Collector

---

## Key Flows
### Payment Flow
![System Architecture](/assets/payments_flow.png)
*Velet - Payment full flow diagram*

---

## Tech Stack

| Category | Tech |
|---|---|
| Language / Framework | Java, Spring Boot |
| Database | PostgreSQL |
| Cache | Redis |
| Messaging | RabbitMQ |
| Observability | OpenTelemetry, Prometheus, Grafana |
| Load Testing | k6 |

---

## Getting Started

```bash
# Each service has its own .env
cp services/gateway/.env.example services/gateway/.env
cp services/identity/.env.example services/identity/.env
cp services/payment/.env.example services/payment/.env
cp services/wallet/.env.example services/wallet/.env
```

```bash
# start dependencies (Postgres, Redis, RabbitMQ)
docker-compose up -d
```

```bash
# Start full infra with observability stack (Prometheus, Grafana, OTel Collector)
docker-compose -f docker-compose.obs.yml up -d
```

Once env files and infra are up, run all services — each in its own window:
```bash
# Windows
scripts\dev-up.bat
```

```bash
# Linux / macOS
./scripts/dev-up.sh
```

---

## What's Implemented

- Identity service (login, register, logout)
- Wallet balance management (reserve, confirm, release)
- Payment service (sync + async processing paths)
- Distributed locking for concurrent wallet operations
- Idempotent payment processing
- Transactional Outbox + event-driven confirmation
- Observability stack (tracing, metrics, dashboards) across all services

This project was developed incrementally as time allowed — the sections above reflect what's actually running today.
