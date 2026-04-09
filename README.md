# Real-Time Analytics & Order Management System

A production-grade distributed system built with **Spring Boot**, **Apache Kafka**, and **PostgreSQL** that demonstrates real-world event-driven architecture, the SAGA pattern, and distributed systems engineering at scale.

---

## What This System Does

This system processes e-commerce orders across 4 independent microservices using Kafka as the message bus. Each service is independently deployable, owns its own data, and communicates exclusively through events; no direct REST calls between services.

A single order flows through stock reservation, payment processing, and real-time analytics, all asynchronously, with full compensation logic if anything fails.

---

## Architecture

```
Client
  │
  ▼
┌─────────────────┐
│  order-service  │  POST /orders → saves to DB → publishes OrderCreated
│   port: 8081    │  Listens for CONFIRMED / CANCELLED → updates order status
└────────┬────────┘
         │ Kafka: orders topic
         ▼
┌─────────────────┐
│  stock-service  │  Checks inventory → reserves stock
│   port: 8083    │  Publishes STOCK_RESERVED or STOCK_FAILED
│                 │  Listens for PAYMENT_FAILED → releases reservation
│                 │  Listens for PAYMENT_SUCCESS → commits stock sale
└────────┬────────┘
         │ Kafka: stock-reserved topic
         ▼
┌──────────────────┐
│ payment-service  │  Processes payment (only after stock is confirmed)
│   port: 8082     │  Publishes PAYMENT_SUCCESS or PAYMENT_FAILED
└────────┬─────────┘
         │ Kafka: payment-success / payment-failed topics
         ▼
┌────────────────────┐
│ analytics-service  │  Listens to ALL topics
│   port: 8084       │  Stores every event in PostgreSQL
│                    │  Caches hot metrics in Redis (30s TTL)
│                    │  Exposes GET /analytics/summary
└────────────────────┘
```

---

## Complete Event Flow

### Happy Path
```
POST /orders
  → order saved (status: NEW)
  → OrderCreated published to Kafka
  → stock-service checks inventory → STOCK_RESERVED
  → payment-service processes payment → PAYMENT_SUCCESS
  → order-service updates status → CONFIRMED
  → analytics-service logs all events
```

### Payment Failure (Compensating Transaction)
```
POST /orders
  → stock reserved
  → payment fails → PAYMENT_FAILED published
  → stock-service receives PAYMENT_FAILED → releases reservation
  → order-service updates status → CANCELLED
```

### Stock Failure
```
POST /orders
  → stock insufficient → STOCK_FAILED published
  → payment never attempted
  → order-service updates status → CANCELLED
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.5.1 |
| Messaging | Apache Kafka 3.9 |
| Database | PostgreSQL 15 |
| Cache | Redis 7.2 |
| ORM | Spring Data JPA / Hibernate |
| Infrastructure | Docker + Docker Compose |
| Language | Java 21 |

---

## Key Engineering Concepts Implemented

### 1. SAGA Pattern (Choreography-based)
Each service reacts to events and publishes its own; no central orchestrator. Compensating transactions automatically undo previous steps when a later step fails. Stock is released if payment fails. Payment is never attempted if stock fails.

### 2. Reserve-Commit-Release Inventory
Stock is reserved (not deducted) when an order arrives. It is committed (reservation cleared) when payment succeeds, or released (returned to available) when payment fails. This prevents overselling without locking inventory unnecessarily.

### 3. Redis Idempotency
Kafka guarantees at-least-once delivery; the same message can be delivered more than once. Before processing any message, each service checks Redis for a key in the format `service:eventType:orderId`. If the key exists, the message is a duplicate and is skipped. Keys expire after 24 hours automatically.

### 4. Dead Letter Queue (DLQ)
Messages that fail processing after 3 retries (with 2-second backoff) are automatically routed to a `*-dlt` topic. A DLT consumer logs the full message details for manual inspection and replay. No message is ever silently lost.

### 5. Redis Caching for Analytics
The `/analytics/summary` endpoint checks Redis before hitting PostgreSQL. On a cache miss, it queries the DB and caches the result for 30 seconds. Every new event invalidates the cache, ensuring freshness without sacrificing performance.

### 6. Kafka Partition Strategy
All topics are configured with 3 partitions. Messages are keyed by `orderId`, ensuring all events for the same order go to the same partition (guaranteed ordering per order). With 3 partitions, running 3 consumer instances gives 3x horizontal throughput with zero code changes.

---

## Kafka Topics

| Topic | Published By | Consumed By |
|---|---|---|
| `orders` | order-service | stock-service, analytics-service |
| `stock-reserved` | stock-service | payment-service |
| `stock-failed` | stock-service | order-service, analytics-service |
| `payment-success` | payment-service | order-service, stock-service, analytics-service |
| `payment-failed` | payment-service | order-service, stock-service, analytics-service |
| `orders-dlt` | Spring Kafka DLQ | stock-service DLT consumer |

---

## Running Locally

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker + Docker Compose

### Step 1 — Start Infrastructure
```bash
docker compose up -d
```
This starts Kafka, Zookeeper, PostgreSQL, and Redis.

### Step 2 — Start Services (4 terminals)
```bash
# Terminal 1
cd order-service && mvn spring-boot:run

# Terminal 2
cd payment-service && mvn spring-boot:run

# Terminal 3
cd stock-service && mvn spring-boot:run

# Terminal 4
cd analytics-service && mvn spring-boot:run
```

### Step 3 — Test
```bash
# Create an order (happy path)
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": "customer-1", "productId": "product-1", "quantity": 2, "price": 299.99}'

# Create an order (payment failure - price > 10000)
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": "customer-1", "productId": "product-1", "quantity": 2, "price": 15000.00}'

# Create an order (stock failure)
curl -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"customerId": "customer-1", "productId": "product-1", "quantity": 999, "price": 99.99}'

# Get analytics summary
curl http://localhost:8084/analytics/summary
```

---

## API Reference

### order-service (port 8081)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/orders` | Create a new order |

### analytics-service (port 8084)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/analytics/summary` | Get real-time order metrics |

**Sample analytics response:**
```json
{
  "totalOrders": 24,
  "confirmedOrders": 13,
  "cancelledOrders": 9,
  "stockFailedOrders": 7,
  "totalRevenue": 3699.87
}
```

---

## Design Decisions & Tradeoffs

### Why Kafka over REST calls between services?
Direct REST calls create tight coupling; if the payment service is down, the order service fails. Kafka decouples producers from consumers. A message published to Kafka is guaranteed to be delivered even if the consumer is temporarily unavailable. This gives us fault tolerance and independent deployability.

### Why choreography SAGA over orchestration?
An orchestrator (like a central saga manager) is a single point of failure and becomes a bottleneck. Choreography distributes the decision-making; each service knows what to do when it receives an event. This scales better and is more resilient.

### Why Redis for idempotency keys over PostgreSQL?
A PostgreSQL lookup on every Kafka message would add significant latency and DB load. Redis lookups are microsecond-level in-memory operations. With a 24-hour TTL, keys auto-expire without any cleanup job needed.

### Why stock check before payment?
Charging a customer's card for an out-of-stock item is a poor user experience and requires a refund flow. Checking stock first means payment is only attempted when fulfillment is guaranteed. This reduces unnecessary payment gateway calls and simplifies the compensation logic.

### CAP Theorem implications
This system is **AP (Available + Partition Tolerant)**. Each service can continue operating independently even if other services are down. The tradeoff is eventual consistency; an order's status may be `NEW` briefly before it becomes `CONFIRMED` or `CANCELLED`. This is acceptable for an order processing system where a few hundred milliseconds of inconsistency has no business impact.

### Kafka vs RabbitMQ
Kafka was chosen because it retains messages for configurable periods (7 days by default), allowing replay, supports high-throughput partitioned consumption, and natively supports the consumer group model needed for horizontal scaling. RabbitMQ is better suited for task queues with complex routing, but doesn't provide the same replay and retention guarantees.

---

## Project Structure

```
real-time-analytics/
├── base-domain/           ← Shared Order model and OrderStatus enum
├── order-service/         ← REST API, order lifecycle management
├── payment-service/       ← Payment processing, idempotency
├── stock-service/         ← Inventory management, DLQ, idempotency
├── analytics-service/     ← Event logging, Redis caching, metrics API
└── docker-compose.yml     ← Kafka + Zookeeper + PostgreSQL + Redis
```

---

## Author

**Nakshatra47** — [github.com/Nakshatra47](https://github.com/Nakshatra47)
