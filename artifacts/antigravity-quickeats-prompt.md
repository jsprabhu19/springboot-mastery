# Prompt: build "QuickEats" — a reference-grade Spring Boot microservices platform

## Who you are and what I want

Act as a senior backend architect building a portfolio-grade reference project. The goal is not a toy demo — it's a system thorough enough that someone could study this codebase to prepare for a system-design or backend interview, and clean enough that a backend beginner could read any single service and understand what it does without external help. Every architectural decision should be the kind of decision a real engineering team would defend, and every non-trivial line of code should be commented with *why*, not just *what*.

Optimize jointly for: correctness, realistic production patterns, and readability. Where these trade off, prefer the version a senior engineer would actually ship over the version that's merely clever.

## The product

**QuickEats** — a real-time food ordering and delivery tracking platform. A customer browses restaurants, places an order, pays, and then watches their order move through preparation and live delivery on a map, with push notifications at every status change. This domain is chosen deliberately because it forces genuine inter-service coordination (payment must succeed before delivery is assigned; a failed payment must roll back the order) and genuine real-time behavior (live GPS position updates), rather than services that could secretly just be one REST CRUD app split into folders.

## Services to build

Each is its own Spring Boot application, own Git-style module, own database, own Dockerfile.

1. **discovery-server** — Netflix Eureka. Every other service registers here; nothing hardcodes another service's host/port.
2. **config-server** — Spring Cloud Config, backed by a local `config-repo` folder of YAML files (no need for an actual remote Git server). Centralizes config so changing a property doesn't require a rebuild.
3. **api-gateway** — Spring Cloud Gateway. Single entry point for all client traffic. Responsibilities: route to the right service via Eureka, validate JWTs on protected routes, apply per-user rate limiting backed by Redis, and log every request/response with a correlation ID that gets propagated downstream.
4. **user-service** — registration, login, JWT issuance/refresh, role management (`CUSTOMER`, `RESTAURANT_OWNER`, `DELIVERY_PARTNER`, `ADMIN`). PostgreSQL. This is also where you implement password hashing (BCrypt) and basic account validation, so a beginner reading it learns real auth fundamentals, not a hand-wave.
5. **restaurant-service** — restaurant and menu management, search/filter by cuisine and location. MongoDB (deliberately a document store, to give the codebase a worked example of when NoSQL fits better than relational).
6. **order-service** — the heart of the system. Owns the order lifecycle (`CREATED → PAYMENT_PENDING → CONFIRMED → PREPARING → OUT_FOR_DELIVERY → DELIVERED`, plus `CANCELLED`/`FAILED`). Implements the **saga orchestration pattern**: on order creation it calls payment, and on success it triggers delivery assignment; on failure at any step it runs compensating actions (refund the payment, release the delivery partner, mark the order failed) instead of leaving the system in a half-finished state. PostgreSQL. Publishes domain events (`OrderCreated`, `OrderConfirmed`, `OrderCancelled`, etc.) via the **transactional outbox pattern** — write the event to an `outbox_events` table in the same DB transaction as the order change, then a separate poller/publisher pushes it to Kafka. Explain in code comments why this avoids the classic "DB committed but Kafka publish failed" bug.
7. **payment-service** — processes a (simulated/mocked) payment, enforces **idempotency** via an idempotency key so retried requests can't double-charge, and exposes a refund endpoint used by the order saga's compensating transaction. PostgreSQL.
8. **delivery-service** — assigns the nearest available delivery partner (Redis `GEO` commands for proximity), and streams live partner location to subscribed customers over **WebSocket/STOMP**. This is the real-time core of the app — wire it so a customer's order-tracking screen would receive a live-updating marker, not a polling REST call.
9. **notification-service** — Kafka consumer for order/delivery domain events; fans them out as WebSocket push notifications to the relevant connected user, plus a stub email/SMS sender (just log it — don't integrate a real provider) so the pattern is visible without needing real credentials.

## Cross-cutting platform concerns (must all be present and visibly demonstrated, not just configured silently)

- **Service discovery & config**: every service registers with Eureka and pulls config from config-server.
- **API gateway**: JWT auth filter, Redis-backed rate limiter, request logging with correlation ID propagation (use an MDC-based filter so every log line across every service can be traced back to one user request).
- **Event-driven communication**: Kafka as the backbone for order/payment/delivery/notification events. Use the outbox pattern in at least order-service; explain in a README why polling-publisher-from-outbox beats "publish directly in the request thread."
- **Saga pattern**: orchestrated in order-service, with explicit compensating-transaction code paths, not just a happy path.
- **Resilience**: Resilience4j circuit breakers, retries, and timeouts on every inter-service HTTP call (e.g., order-service calling payment-service). Include a fallback method for at least one circuit breaker so the pattern is visibly exercised, not just declared.
- **Caching**: Redis caching on restaurant-service's menu reads, with a clear cache-invalidation strategy on writes (don't leave it as a footgun).
- **Security**: Spring Security + JWT across services, role-based authorization on endpoints, secrets kept out of source (use environment variables / config-server, never hardcoded).
- **Real-time**: WebSocket/STOMP for delivery tracking and notifications — this is non-negotiable, it's the feature that makes the system "realtime" rather than a generic CRUD demo.
- **Observability**: Micrometer metrics exposed via Actuator and scraped by Prometheus, a Grafana dashboard (at least one, for order throughput/latency), distributed tracing via OpenTelemetry + Zipkin so a single request's trace is visible end-to-end across 3+ services, and centralized logging (ELK or a lighter Loki+Promtail stack is fine) with the correlation ID as a searchable field.
- **Data per service**: don't share a database between services. Use Postgres for relational services, MongoDB for restaurant-service, Redis for caching/geo/rate-limiting. Use Flyway (Postgres services) for versioned schema migrations — no manual SQL setup.
- **API documentation**: springdoc-openapi on every service, so each one has a working `/swagger-ui` you can click through.
- **Containerization**: a Dockerfile per service and one root `docker-compose.yml` that brings up the entire platform (all services + Postgres + MongoDB + Redis + Kafka + Zookeeper + Prometheus + Grafana + Zipkin) with one command. A basic Kubernetes manifest set (Deployments + Services + ConfigMaps) is a welcome stretch goal but Docker Compose is the must-have.
- **CI**: a GitHub Actions workflow that builds, tests, and lints every service on push.

## Non-negotiables for code quality and readability

These apply to every service, every time you touch code:

- Standard layered package structure per service: `controller`, `service`, `repository`, `dto`, `entity`/`document`, `mapper`, `exception`, `config`, `event`. Never let a controller talk to a repository directly.
- DTOs are separate from entities; never expose JPA/Mongo entities directly over the API.
- A global `@ControllerAdvice` exception handler per service returning a consistent error response shape (timestamp, status, error code, message, path) — no raw stack traces leaking to clients.
- Bean Validation (`@Valid`, `@NotNull`, etc.) on every request DTO; don't rely on manual null checks scattered through service methods.
- Constructor injection only — no field injection with `@Autowired` on fields.
- Meaningful names over comments where possible, but every class implementing a named pattern (saga, outbox, circuit breaker, idempotency key, CQRS-style read model, etc.) gets a short Javadoc block explaining *what pattern this is, why it's here, and what would break without it*. This is the single most important rule for the "study this to learn" goal — a beginner should be able to grep for "saga" or "outbox" and land on a comment that actually teaches them the concept.
- No magic numbers/strings — use constants or enums (e.g., order status as an enum, not a string).
- Each service ships its own `README.md`: one paragraph on its responsibility, its API endpoints (or a link to its Swagger UI), the events it publishes/consumes, and how to run it standalone.
- A root-level `README.md` covering: overall architecture (link back to or redraw the diagram), the list of services and their responsibilities, the full list of patterns demonstrated and where to find each one in the code, setup instructions for `docker-compose up`, and a short "if you're studying this for interviews, start here" section pointing at order-service's saga implementation and the outbox table as the two most-asked-about pieces.
- Unit tests (JUnit 5 + Mockito) for service-layer business logic, and integration tests using **Testcontainers** for at least the database- and Kafka-touching paths — don't mock your way around ever actually testing the Kafka flow.
- Consistent code style — pick one (Google Java Style is a safe default) and apply it uniformly; don't let formatting drift between services.

## Build phases

Work through these in order. Don't start real-time/event work before the foundational services exist and talk to each other over plain REST — get the boring parts right first.

1. **Foundation**: discovery-server, config-server, api-gateway skeleton, and a shared parent Maven/Gradle build. Get all three running together with one service (user-service) registering and being routable through the gateway.
2. **Core CRUD services**: user-service (with JWT auth), restaurant-service. Plain REST, no events yet. Get Swagger working on both.
3. **Order + payment with the saga**: order-service and payment-service, synchronous REST calls between them first, with the saga orchestration and compensating transactions fully working end to end before you add Kafka.
4. **Event-driven layer**: introduce Kafka, convert order-service to publish via the outbox pattern, build notification-service as a consumer. Verify an order's full event trail is visible in logs/tracing.
5. **Real-time delivery**: delivery-service with Redis geo-assignment and WebSocket/STOMP live tracking; wire notification-service to also push over WebSocket.
6. **Resilience and security hardening**: Resilience4j circuit breakers/retries on all inter-service calls, full RBAC across endpoints, idempotency keys on payment-service.
7. **Observability**: Actuator + Prometheus + Grafana dashboard, OpenTelemetry + Zipkin tracing, centralized logging with correlation IDs.
8. **Packaging and polish**: Dockerfiles, the unified docker-compose.yml, GitHub Actions CI, the root README, and a final pass making sure every "pattern" class has its explanatory Javadoc.

## Definition of done

The platform comes up cleanly with `docker-compose up`, a customer can register, browse a restaurant's menu, place an order, watch it go through a (mocked) payment, see it assigned to a delivery partner, and watch a live-updating delivery position on a tracking endpoint while receiving push notifications at each status change — all observable end-to-end in Zipkin as a single distributed trace, with metrics visible in Grafana. Someone with no prior context should be able to read the root README, then open order-service, and understand the saga and outbox pattern well enough to explain it in an interview.
