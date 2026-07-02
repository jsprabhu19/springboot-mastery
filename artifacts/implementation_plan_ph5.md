# Implementation Plan - Phase 5: Real-time Delivery System

This phase implements the real-time food delivery tracking system for **QuickEats**. It comprises creating a new microservice `delivery-service` that handles proximity matching of delivery partners using Redis Geo commands, persistent Postgres records for delivery runs, and real-time STOMP WebSockets streaming coordinate updates to clients. Additionally, we will add STOMP WebSocket support to `notification-service` to fan out order event push notifications.

---

## User Review Required

> [!IMPORTANT]
> **Redis Infrastructure**: We will leverage the existing Redis container running on port `6379` for executing Geo operations (`GEOADD`, `GEOSEARCH`) and managing active delivery partner coordinates.
>
> **Kafka Events**: `delivery-service` will subscribe to the `order-events` Kafka topic to automatically trigger delivery matching when an order transitions to `PAID`.
>
> **Database**: `delivery-service` will require a PostgreSQL database `quickeats_delivery`. We will update `docker/postgres/init-db.sql` to initialize it and configure a Flyway migration script inside `delivery-service`.

---

## Proposed Changes

### Parent Pom & Configuration Repository

#### [MODIFY] [pom.xml](file:///e:/Learning/antigravity-projects/springboot-app/pom.xml)
- Declares the new `delivery-service` module in the `<modules>` list.

#### [NEW] [delivery-service.yml](file:///e:/Learning/antigravity-projects/springboot-app/config-repo/delivery-service.yml)
- Configures port `8086`, database connections, Eureka registration, Kafka properties, and Redis host mappings.

#### [MODIFY] [notification-service.yml](file:///e:/Learning/antigravity-projects/springboot-app/config-repo/notification-service.yml)
- Exposes port mappings for WebSockets configuration if needed.

#### [MODIFY] [api-gateway.yml](file:///e:/Learning/antigravity-projects/springboot-app/config-repo/api-gateway.yml)
- Adds WebSocket route routing mapping:
  - Routing `/ws-delivery/**` to `delivery-service`
  - Routing `/ws-notifications/**` to `notification-service`
  - Ensures gateway routes configure HTTP/WebSocket headers appropriately.

---

### Database Configurations

#### [MODIFY] [init-db.sql](file:///e:/Learning/antigravity-projects/springboot-app/docker/postgres/init-db.sql)
- Seed command to create database `quickeats_delivery`.

---

### Delivery Service (`delivery-service`)

#### [NEW] [pom.xml](file:///e:/Learning/antigravity-projects/springboot-app/delivery-service/pom.xml)
- Configures Spring Boot application with Web, Discovery Client, Config Client, JPA, Redis, WebSocket, Kafka, Flyway, and PostgreSQL dependencies.

#### [NEW] [application.yml](file:///e:/Learning/antigravity-projects/springboot-app/delivery-service/src/main/resources/application.yml)
- Configuration bootstrap pointing to Config Server.

#### [NEW] [DeliveryServiceApplication.java](file:///e:/Learning/antigravity-projects/springboot-app/delivery-service/src/main/java/com/quickeats/deliveryservice/DeliveryServiceApplication.java)
- Entry point of the microservice.

#### [NEW] [V1__create_delivery_table.sql](file:///e:/Learning/antigravity-projects/springboot-app/delivery-service/src/main/resources/db/migration/V1__create_delivery_table.sql)
- Schema DDL creating the `deliveries` table.

#### [NEW] [WebSocketConfig.java](file:///e:/Learning/antigravity-projects/springboot-app/delivery-service/src/main/java/com/quickeats/deliveryservice/config/WebSocketConfig.java)
- Configures WebSocket Message Broker mapping the endpoint `/ws-delivery` and destination prefix `/topic`.

#### [NEW] [RedisConfig.java](file:///e:/Learning/antigravity-projects/springboot-app/delivery-service/src/main/java/com/quickeats/deliveryservice/config/RedisConfig.java)
- Connection definitions mapping RedisTemplate for custom Geo queries.

#### [NEW] [Delivery.java](file:///e:/Learning/antigravity-projects/springboot-app/delivery-service/src/main/java/com/quickeats/deliveryservice/entity/Delivery.java) & `DeliveryStatus.java`
- Persistent JPA entity mapping order-to-partner assignment statuses.

#### [NEW] [OrderEventListener.java](file:///e:/Learning/antigravity-projects/springboot-app/delivery-service/src/main/java/com/quickeats/deliveryservice/listener/OrderEventListener.java)
- Listens to Kafka `order-events` topic. When an order transitions to `PAID`, triggers proximity-based matching.

#### [NEW] [DeliveryService.java](file:///e:/Learning/antigravity-projects/springboot-app/delivery-service/src/main/java/com/quickeats/deliveryservice/service/DeliveryService.java) & `DeliveryServiceImpl.java`
- Core matching algorithm:
  - Fetches restaurant location by calling `restaurant-service` via RestTemplate.
  - Queries Redis Geo Set (`delivery:partners:active`) for the nearest partner within 5km.
  - Creates the database delivery record.
  - Removes the partner from the active pool to mark them as busy.
  - Exposes location update methods pushing coordinates to STOMP topic `/topic/delivery/{orderId}`.

#### [NEW] [DeliveryController.java](file:///e:/Learning/antigravity-projects/springboot-app/delivery-service/src/main/java/com/quickeats/deliveryservice/controller/DeliveryController.java)
- REST endpoints:
  - `POST /api/v1/deliveries/partners/active` (adds active partner location coordinates to Redis).
  - `POST /api/v1/deliveries/{id}/location` (updates partner coordinate during transport and streams to WebSocket).
  - `POST /api/v1/deliveries/{id}/complete` (completes delivery run).

---

### Notification Service (`notification-service`)

#### [NEW] [WebSocketConfig.java](file:///e:/Learning/antigravity-projects/springboot-app/notification-service/src/main/java/com/quickeats/notificationservice/config/WebSocketConfig.java)
- Configures WebSocket Message Broker endpoint `/ws-notifications` sending notifications to `/topic/notifications/{userId}`.

#### [MODIFY] [OrderEventListener.java](file:///e:/Learning/antigravity-projects/springboot-app/notification-service/src/main/java/com/quickeats/notificationservice/listener/OrderEventListener.java)
- Injects `SimpMessagingTemplate` to publish real-time notification alerts downstream.

---

## Verification Plan

### Automated / Manual Verification

1. **Active Partner Seeding**:
   - Seed an active delivery partner coordinate in Redis near restaurant location:
     `POST /api/v1/deliveries/partners/active` with `{"partnerId": "delivery-guy-1", "latitude": 12.9716, "longitude": 77.5946}`.
2. **Order Placement & Matching**:
   - Place an order. Once payment transitions to `PAID`, verify `delivery-service` matches `delivery-guy-1` automatically:
     - Verify database persistent record in `delivery-service` has status `ASSIGNED`.
3. **Live Track WebSocket updates**:
   - Connect to `ws://localhost:8080/ws-delivery` via STOMP and subscribe to `/topic/delivery/{orderId}`.
   - Send location update request:
     `POST /api/v1/deliveries/{deliveryId}/location` with `{"latitude": 12.9725, "longitude": 77.5955}`.
   - Verify WebSocket client instantly receives location payload update.
4. **Complete Delivery**:
   - Complete delivery run and verify final statuses.
