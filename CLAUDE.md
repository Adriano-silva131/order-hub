# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OrderHub is a distributed e-commerce order platform built with a microservices architecture. Each service is an independent Spring Boot 4.0.3 + Java 21 Gradle project (no root build file). All services expose `/actuator/prometheus` for metrics.

## Running the Full Stack

The entire infrastructure (databases, Kafka, Keycloak, Redis, Prometheus, Grafana, and all services) is managed via Docker Compose:

```bash
cd infra
docker compose up -d --build
```

Key URLs when running via Docker Compose:
- API Gateway: `http://localhost:8000`
- Keycloak: `http://localhost:9090` (realm: `orderhub`)
- Grafana: `http://localhost:3000` (the "JVM (Micrometer)", ex-dashboard ID 4701, is auto-provisioned via `infra/grafana/provisioning`)
- Prometheus: `http://localhost:9091`

## Per-Service Development Commands

Each service has its own Gradle wrapper. Commands must be run from the service directory:

```bash
cd <service-name>

# Build (skip tests)
./gradlew build -x test

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.adriano.orderhub.controller.OrderControllerTest"

# Run the service locally (requires local infrastructure)
./gradlew bootRun
```

For local development, services use `application-local.properties` (activated via `spring.profiles.active=local`). The `local` profile assumes infrastructure is accessible at localhost with the ports defined in `docker-compose.yml`.

## Load Testing (k6)

A k6 load test script is in `k6/load-test.js`. It requires a valid JWT and targets the API Gateway:

```bash
# Against Minikube (default BASE_URL is the Minikube NodePort)
k6 run -e JWT_TOKEN=<token> k6/load-test.js

# Against Docker Compose
k6 run -e BASE_URL=http://localhost:8000 -e JWT_TOKEN=<token> k6/load-test.js
```

The script runs a 4-stage ramp (warm-up → steady → spike → cool-down) and asserts p95 latency < 1s and error rate < 15%.

## Architecture

### Request Flow

```
Client → API Gateway (8000) → order-service (8080) / catalog-service (8081)
```

- **Authentication offloading**: JWT validation happens exclusively at the API Gateway (Spring WebFlux + Spring Security OAuth2). Internal services have no security layer.
- **Rate limiting**: Token Bucket algorithm via Redis, keyed by the `sub` claim from the JWT.
- **Internal routing**: The gateway proxies to services by container name (Docker) or localhost (local dev).

### Order Status Lifecycle

```
PENDING_PAYMENT → PAID       (payment.processed.v1 with status=APPROVED)
PENDING_PAYMENT → CANCELLED  (payment.processed.v1 with status≠APPROVED)
```

### Event-Driven Flow (Choreographed Saga)

```
order-service → [order-events]   → payment-service          (event-type: order.created.v1)
payment-service → [payment-events] → order-service (status update)
                                   → notification-service (email)  (event-type: payment.processed.v1)
```

Event records are Java `record` types defined in each service's `event/` package. Both `order-service` and `payment-service` carry their own local copies of the event types they produce or consume — there is no shared library.

### Kafka Consumer Pattern

`order-service` and `payment-service` share `GenericEventConsumer`:
- A single `@KafkaListener` dispatches to registered `EventHandler<T>` beans, resolved by the `event-type` header.
- `@RetryableTopic` (4 attempts, exponential backoff 2s→10s) handles transient failures.
- `@DltHandler` persists exhausted messages to a `dlt_messages` table for later reprocessing via `DltReprocessingService`.
- To add a new event handler: implement `EventHandler<T>` and register it as a Spring bean — the consumer auto-discovers it.

`notification-service` uses a simpler `NotificationConsumer` with two explicit `@KafkaListener` methods (one per topic), separate container factories, and no DLT/retry infrastructure.

### Service Responsibilities

| Service | Port | DB | Notes |
|---|---|---|---|
| `api-gateway` | 8000 | Redis | Spring WebFlux, reactive. No business logic. |
| `order-service` | 8080 | PostgreSQL | Feign + Resilience4j circuit breaker to catalog-service. Flyway migrations. |
| `catalog-service` | 8081 | MongoDB | Flexible product schema. No Kafka. |
| `payment-service` | 8082 | PostgreSQL | Idempotency check via `existsByOrderId`. Flyway migrations. |
| `notification-service` | 8083 | — | Consumes both `order-events` and `payment-events`. Sends email via Gmail SMTP. |

### Database Migrations

`order-service` and `payment-service` use Flyway. Migrations live in `src/main/resources/db/migration/`. The local profile sets `ddl-auto=update`; production uses `ddl-auto=validate`.

### Circuit Breaker

Configured on `order-service` for calls to `catalog-service` via Feign (`spring.cloud.openfeign.circuitbreaker.enabled=true`). Circuit breaker instance name is `catalog-service`. Settings are in `application-local.properties`.

### Rate Limiter

Configured in `api-gateway/src/main/resources/application.yml`. Default values: `replenishRate=2`, `burstCapacity=5`, `requestedTokens=1`. Requests are keyed by the `sub` claim extracted from the JWT. Expect HTTP 429 during load tests once burst capacity is exceeded.

## Testing Approach

- **Controller tests**: `@WebMvcTest` + `@MockitoBean` for the service layer. No DB or Kafka needed.
- **Service/Mapper tests**: Plain unit tests with Mockito.
- **Integration tests**: Use `@DataMongoTest` (catalog-service) or H2 in-memory (order-service/payment-service JPA tests).
- The CI pipeline runs `./gradlew build -x test` for each service in parallel using a matrix strategy.

Spring Boot 4 replaces `@MockBean` with `@MockitoBean` — use the latter in all test classes.

## Kubernetes

The `k8s/` directory contains manifests for deploying the full stack to a cluster (tested with Minikube):

```
k8s/
├── namespace.yaml        # orderhub namespace
├── secrets.yaml          # DB credentials, Gmail SMTP, Keycloak client secret
├── apps/                 # one Deployment + Service per microservice
├── infra/                # Kafka, Keycloak, MongoDB, PostgreSQL, Redis
└── monitoring/           # Prometheus and Grafana
```

Apply in order: namespace → secrets → infra → apps → monitoring.

## Key Configuration Properties

| Property | Purpose |
|---|---|
| `integration.catalog.url` | Feign base URL for catalog-service (order-service only) |
| `dlt.reprocess.interval` | Milliseconds between DLT reprocessing attempts (default: 60000) |
| `spring.kafka.consumer.group-id` | Consumer group, set per service |

Kafka messages carry custom headers: `event-type`, `event-version`, `occurred-at`. The consumer routes based on `event-type`.
