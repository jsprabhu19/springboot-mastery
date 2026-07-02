# Implementation Plan: QuickEats Microservices Platform (Updated)

Build **QuickEats**, a portfolio-grade reference Spring Boot microservices platform. The system uses **Java 25**, **Spring Boot 3.3.x**, and **Spring Cloud 2023.0.x**. It integrates with **Razorpay (test mode)** for payment processing, **Resend** for email notifications, and manages database schema updates using **Flyway**. Backing services (Postgres, MongoDB, Redis, Kafka, Zipkin, Prometheus, Grafana) will run in Docker containers via Docker Compose.

---

## User Decisions & Clarifications

### Local Java Environment
- **Java Version**: We will use **Java 25** for all modules.
- **Build System**: A unified **Maven** multi-module structure with a parent `pom.xml` at the root.

### Backing Infrastructure (Docker Desktop)
- Since Docker Desktop is installed and running locally, **you do not need to install PostgreSQL, MongoDB, Redis, or Kafka/Zookeeper on your host machine**.
- We will provide a root `docker-compose.yml` that runs these services in Docker containers.
- We will configure a Docker Compose profile (or standalone setup) so you can run the backing databases and message brokers in the background while debugging or running the Spring Boot services in your IDE (or terminal).

### Payment Integration (Razorpay Test Mode)
- **Payment Service** will integrate with Razorpay using their official Java SDK (or REST API) running in **test mode**.
- Transactions will verify signatures and process payments against Razorpay's test environment.
- The refund logic (saga compensation path) will call the Razorpay Refund API.

### Email Notification (Resend Free Tier)
- **Notification Service** will integrate with **Resend** to send real-world emails using their free tier API key.
- We will configure the API key securely using environment variables / Spring Cloud Config.

---

## Proposed Project Structure

```text
springboot-app/
├── pom.xml                                   # Root parent POM
├── docker-compose.yml                        # Starts Postgres, Mongo, Redis, Kafka, etc.
├── discovery-server/                         # Eureka Discovery Server
├── config-server/                            # Spring Cloud Config Server
├── config-repo/                              # Local git-like directory for config YAMLs
├── api-gateway/                              # Spring Cloud Gateway
├── user-service/                             # User profiles & security (Postgres)
├── restaurant-service/                       # Restaurant & Menu CRUD (MongoDB, Redis Cache)
├── order-service/                            # Saga Orchestrator & Outbox Pattern (Postgres)
├── payment-service/                          # Razorpay Test Mode integration (Postgres)
├── delivery-service/                         # Redis Geo matching & Websocket live tracking
├── notification-service/                     # Resend emails & Websocket alerts (Kafka consumer)
├── prometheus/                               # Prometheus configuration scraper
├── grafana/                                  # Grafana dashboards provisioning
└── .github/workflows/ci.yml                  # GitHub Actions workflow (Java 25)
```

---

## Proposed Changes

### Parent POM & CI Workflow

#### [NEW] [pom.xml](file:///e:/Learning/antigravity-projects/springboot-app/pom.xml)
Parent Maven POM configured with:
- `<java.version>25</java.version>`
- Dependency management for Spring Boot Starter Parent `3.3.x`
- Dependency management for Spring Cloud Dependencies `2023.0.x`
- Declared child modules for all 9 services.

#### [NEW] [ci.yml](file:///e:/Learning/antigravity-projects/springboot-app/.github/workflows/ci.yml)
GitHub Actions workflow executing compilation and integration tests using Java 25 runner.

---

### Core Infrastructure Services (Phase 1)

#### [NEW] [discovery-server](file:///e:/Learning/antigravity-projects/springboot-app/discovery-server)
Netflix Eureka Service discovery. Runs on port `8761`.

#### [NEW] [config-server](file:///e:/Learning/antigravity-projects/springboot-app/config-server)
Spring Cloud Config Server running on port `8888`. Backed by native local profiles pointing to `../config-repo`.

#### [NEW] [config-repo](file:///e:/Learning/antigravity-projects/springboot-app/config-repo)
Configuration repository. Contains YAML files containing service configurations.
- API keys (Razorpay, Resend) will be mapped to environment variables (e.g., `${RAZORPAY_KEY_ID}`, `${RESEND_API_KEY}`) to prevent hardcoding secrets.

#### [NEW] [api-gateway](file:///e:/Learning/antigravity-projects/springboot-app/api-gateway)
Spring Cloud Gateway (port `8080`).
- **JWT Filter**: Validates bearer tokens against user-service signatures and passes claims via downstream request headers.
- **Correlation ID Filter**: Intercepts requests, generates/extracts a unique Correlation ID, puts it in MDC, and forwards it to downstream headers (`X-Correlation-Id`).
- **Redis Rate Limiter**: Implements token bucket rate-limiting via reactive Redis.

---

### Business Services (Phases 2-5)

#### [NEW] [user-service](file:///e:/Learning/antigravity-projects/springboot-app/user-service)
User authentication service (PostgreSQL, Port `8081`).
- Uses BCrypt password hashing.
- Issues JWT tokens for authentication.
- Uses Flyway to manage the user credentials schema.

#### [NEW] [restaurant-service](file:///e:/Learning/antigravity-projects/springboot-app/restaurant-service)
Restaurant metadata and menu management (MongoDB, Redis Caching, Port `8082`).
- Implements standard Spring Data MongoDB repositories.
- Employs Redis cache for high-read menu lookups with eviction annotations on updates.

#### [NEW] [order-service](file:///e:/Learning/antigravity-projects/springboot-app/order-service)
Core order lifecycle orchestrator (PostgreSQL, Port `8083`).
- **Saga Orchestrator**: Handles order execution state machine. Initiates Razorpay payment processing and handles compensatory operations (payment refunds, order cancellation) if downline failures occur.
- **Transactional Outbox**: Writes `order_events` to a dedicated outbox table in Postgres within the same transaction. A background scheduler polls this table, publishes to Kafka, and marks events as processed.

#### [NEW] [payment-service](file:///e:/Learning/antigravity-projects/springboot-app/payment-service)
Razorpay Payment Processor (PostgreSQL, Port `8084`).
- Integrates with the Razorpay API in Test Mode.
- Implements strict idempotency checking via `X-Idempotency-Key` headers stored in the database.
- Implements compensation refund endpoint linked to Razorpay's refund API.

#### [NEW] [delivery-service](file:///e:/Learning/antigravity-projects/springboot-app/delivery-service)
Delivery matching and real-time tracking (Redis Geo, WebSockets, Port `8085`).
- Uses Redis GEO commands (`GEOADD`, `GEORADIUS` / `GEOSEARCH`) to map nearest active delivery partner.
- Exposes STOMP over WebSocket connection endpoint to stream live partner coordinates updates to client maps.

#### [NEW] [notification-service](file:///e:/Learning/antigravity-projects/springboot-app/notification-service)
Notification fanner-out (Kafka Consumer, Resend Email SDK, WebSockets, Port `8086`).
- Consumes events from Kafka broker.
- Sends actual emails using the **Resend** API.
- Sends live app notifications over WebSockets/STOMP.

---

### Observability & Hardening (Phases 6-7)

#### [NEW] [prometheus.yml](file:///e:/Learning/antigravity-projects/springboot-app/prometheus/prometheus.yml)
Prometheus scrapers targeting Spring Actuator metrics.

#### [NEW] [grafana/provisioning](file:///e:/Learning/antigravity-projects/springboot-app/grafana/provisioning)
Provisioned dashboard config for Order throughput, latencies, and service health metrics.

---

## Verification Plan

### Booting Backing Services (No Local Install Needed)
Run only the infrastructure services (Postgres, Mongo, Redis, Kafka, Zipkin, Prometheus, Grafana) in Docker:
```bash
docker-compose up -d postgres mongodb redis kafka zipkin prometheus grafana
```
This boots all backend databases and message brokers, allowing you to run, test, and debug the Spring Boot services locally inside your IDE or using the terminal.

### Automated Tests
Run integration tests using Testcontainers to dynamically spin up matching ephemeral containers during execution:
```bash
mvn clean verify
```
This validates outbox events, payments, and geo-matching end-to-end.

### Manual Verification
1. Boot the entire platform:
   ```bash
   docker-compose up --build -d
   ```
2. Verify all services registered at Eureka dashboard: `http://localhost:8761`.
3. Open OpenAPI UI via Gateway: `http://localhost:8080/swagger-ui.html`.
4. Trigger an order flow via REST client (e.g. Postman), verify payment captures on Razorpay test dashboard, verify email arrives via Resend log, and view the distributed trace end-to-end on Zipkin: `http://localhost:9411`.
