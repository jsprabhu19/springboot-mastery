# Implementation Plan - Deploy QuickEats Microservices to Docker

This plan outlines the steps required to deploy the entire QuickEats microservices platform to Docker, enabling automated orchestration of the core infrastructure and all 9 Spring Boot microservices.

---

## User Review Required

> [!NOTE]
> * **Build Strategy**: This plan uses a local build strategy. We will build the `.jar` packages on the host machine using Maven (`./mvnw clean package -DskipTests`) and then copy them into lightweight alpine JRE container images. This is much faster and resource-efficient for local development compared to building everything inside a multi-stage Docker container.
> * **Port Mapping (Host vs. Container)**:
>   * **Internal Container URL**: Within the internal Docker bridge network, containers communicate directly using container names (e.g., `http://discovery-server:8761/eureka/`).
>   * **External Host URL (`localhost`)**: Because we map the container ports to your physical host machine (e.g., `8080:8080`), you will still access them from your browser on your host machine using `http://localhost:<PORT>`. If you are deploying this to a remote server or a VM, you would replace `localhost` with the server's IP address or domain name.
> * **No Frontend UI**: QuickEats is a **pure backend microservices platform** (REST APIs + WebSockets). There is no standalone graphical frontend web application. All API actions (placing orders, auth, payments) are executed through the API Gateway REST endpoints or via the Swagger UI.

---

## Proposed Changes

### Dockerfiles

We will add a standard `Dockerfile` in each of the 9 microservice folders. Each Dockerfile will follow a lightweight JRE 21 alpine pattern:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE <PORT>
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### [NEW] [discovery-server/Dockerfile](file:///e:/Learning/antigravity-projects/springboot-app/discovery-server/Dockerfile) (Port 8761)
#### [NEW] [config-server/Dockerfile](file:///e:/Learning/antigravity-projects/springboot-app/config-server/Dockerfile) (Port 8888)
#### [NEW] [api-gateway/Dockerfile](file:///e:/Learning/antigravity-projects/springboot-app/api-gateway/Dockerfile) (Port 8080)
#### [NEW] [user-service/Dockerfile](file:///e:/Learning/antigravity-projects/springboot-app/user-service/Dockerfile) (Port 8081)
#### [NEW] [restaurant-service/Dockerfile](file:///e:/Learning/antigravity-projects/springboot-app/restaurant-service/Dockerfile) (Port 8082)
#### [NEW] [order-service/Dockerfile](file:///e:/Learning/antigravity-projects/springboot-app/order-service/Dockerfile) (Port 8083)
#### [NEW] [payment-service/Dockerfile](file:///e:/Learning/antigravity-projects/springboot-app/payment-service/Dockerfile) (Port 8084)
#### [NEW] [notification-service/Dockerfile](file:///e:/Learning/antigravity-projects/springboot-app/notification-service/Dockerfile) (Port 8085)
#### [NEW] [delivery-service/Dockerfile](file:///e:/Learning/antigravity-projects/springboot-app/delivery-service/Dockerfile) (Port 8086)

---

### Docker Compose Orchestration

#### [MODIFY] [docker-compose.yml](file:///e:/Learning/antigravity-projects/springboot-app/docker-compose.yml)

We will expand the root `docker-compose.yml` to orchestrate all services. 

Key enhancements include:
1. **Health Checks**: Add health checks to backing services (`postgres`, `mongodb`, `redis`, `kafka`, `zipkin`) to guarantee they are fully healthy before boot starting any Spring Boot service.
2. **Service Definitions**: Add all 9 services to the compose file.
3. **Environment Overrides**: Override `localhost` properties with container names using Spring Boot environment variable mapping rules:
   - `SPRING_CONFIG_IMPORT: "optional:configserver:http://config-server:8888"`
   - `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: "http://discovery-server:8761/eureka/"`
   - `SPRING_DATASOURCE_URL` (for PostgreSQL users, orders, payments, delivery)
   - `SPRING_DATA_MONGODB_URI` (for Restaurant MongoDB)
   - `SPRING_DATA_REDIS_HOST: redis`
   - `SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092`
   - `MANAGEMENT_ZIPKIN_TRACING_ENDPOINT: http://zipkin:9411/api/v2/spans`
4. **Volumes**: Mount the host's `config-repo` directory into the `config-server` container at `/app/config-repo` so it can serve the native configurations.
5. **Startup Ordering (`depends_on`)**:
   - `discovery-server` depends on backing services.
   - `config-server` depends on `discovery-server`.
   - All core services (`api-gateway`, `user-service`, etc.) depend on `config-server` and `discovery-server`.

---

## Verification & Usage Plan

### Deploying the Platform

1. **Clean and package the whole project** using Maven:
   ```powershell
   ./mvnw clean package -DskipTests
   ```
2. **Build and start all Docker services**:
   ```powershell
   docker-compose up --build -d
   ```

### Accessing the QuickEats Application

The primary entry point to access the **QuickEats** APIs and documentation is through the **API Gateway** running on port `8080`:

| Service / Interface | Access URL (Host Browser) | Internal Docker URL | Description |
| :--- | :--- | :--- | :--- |
| **API Entrypoint** | `http://localhost:8080/api/v1/...` | `http://api-gateway:8080/api/v1/...` | Base path for all microservice REST APIs (User, Restaurant, Order, Payment, etc.). |
| **QuickEats Swagger UI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | N/A | **Interactive graphical web interface** to explore and execute API endpoints (creating users, placing orders, making payments). |
| **Eureka Discovery Dashboard** | [http://localhost:8761](http://localhost:8761) | `http://discovery-server:8761` | Service registry control panel. |
| **Zipkin Distributed Tracing** | [http://localhost:9411](http://localhost:9411) | `http://zipkin:9411` | Request trace analysis dashboard. |
| **Prometheus Metrics** | [http://localhost:9090](http://localhost:9090) | `http://prometheus:9090` | Microservice performance and health targets. |
| **Grafana Dashboard** | [http://localhost:3000](http://localhost:3000) | `http://grafana:3000` | Pre-built QuickEats metrics dashboard (admin/admin). |
| **STOMP WebSockets** | `ws://localhost:8080/ws-delivery` | `ws://delivery-service:8086/ws-delivery` | Real-time WebSocket connection to track deliveries. |
