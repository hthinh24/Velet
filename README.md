# Velet — Payment & Wallet Core Infrastructure

> A payment & wallet infrastructure focused on consistency-heavy distributed systems patterns. Current scope: Identity,
> Wallet, and Payment core services — built with an eye toward scaling into a broader super-app-style platform later.

---

## Overview

Velet models the core money-movement layer of a super-app style platform: user identity, wallet balance management,
payment reservation/confirmation, and transfer between users and merchants. The focus is on **correctness under
concurrency** — safe balance updates, idempotent retries, and consistent state across services under load — rather than
breadth of features.

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

| Category                 | Tech                               |
|--------------------------|------------------------------------|
| **Language / Framework** | Java, Spring Boot (4.0.6)          |
| **Database**             | PostgreSQL                         |
| **Cache**                | Redis                              |
| **Messaging**            | RabbitMQ                           |
| **Observability**        | OpenTelemetry, Prometheus, Grafana |
| **API Documentation**    | SpringDoc OpenAPI / Swagger UI     |
| **Database Migration**   | Flyway                             |
| **Load Testing**         | k6                                 |

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

K6 load testing script:
```bash
k6 run --out web-dashboard=open scripts\k6\load-test.js
```


---

## Endpoints

**Infrastructure**

| Service                    | URL                    | Credentials       |
|----------------------------|------------------------|-------------------|
| RabbitMQ Management UI     | http://localhost:15672 | `admin` / `admin` |
| Grafana                    | http://localhost:3000  | `admin` / `admin` |
| Prometheus                 | http://localhost:9090  | —                 |
| OTel Collector (OTLP gRPC) | localhost:4317         | —                 |
| OTel Collector (OTLP HTTP) | localhost:4318         | —                 |

**API Docs (Swagger)**

| Service  | URL                                         |
|----------|---------------------------------------------|
| Identity | http://localhost:8081/swagger-ui/index.html |
| Wallet   | http://localhost:8082/swagger-ui/index.html |
| Payment  | http://localhost:8083/swagger-ui/index.html |

*(Ports/credentials above come from `.env.example` where available — swagger ports are placeholders, adjust to match
each service's actual port.)*

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

---

## Support

If you like this project, feel free to:

- ⭐ this repository. And we will be happy together :)

Thanks for supporting me!