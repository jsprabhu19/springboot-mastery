# QuickEats Microservices - Functional End-to-End Flows (Beginner's Guide)

Welcome to the QuickEats architecture guide! If you are new to microservices, don't worry. This document explains how the system works behind the scenes using **simple real-world analogies** and **step-by-step descriptions** alongside technical diagrams.

---

## 1. User Authentication & Gateway Session Flow

### 💡 The Analogy: The Movie Ticket
Imagine going to a movie theater. 
1. You show your ID and pay at the ticket counter (**User Service**).
2. They give you a ticket stub (**JWT Token**). 
3. When you walk into the halls (**Downstream Services**), the ticket checkers (**API Gateway**) look at the theater's official stamp on your ticket. They don't need to call the ticket counter to verify who you are every time; they trust the stamp.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Customer (Browser/Postman)
    participant Gateway as Security Gate (API Gateway)
    participant UserSvc as Ticket Office (User Service)
    participant Database as User Database (PostgreSQL)
    participant CoreSvc as Movie Hall (e.g., Order Service)

    Client->>Gateway: 1. Send username & password
    Gateway->>UserSvc: Forward login request
    UserSvc->>Database: Verify credentials
    Database-->>UserSvc: Match found!
    UserSvc-->>Gateway: Generate ticket (JWT) with signature
    Gateway-->>Client: 200 OK (Return JWT)

    Note over Client, Gateway: Subsequent Requests
    Client->>Gateway: 2. Request order list (with JWT)
    Gateway->>Gateway: Check stamp (Validate JWT Signature)
    Gateway->>Gateway: Attach ticket details (userId, role) to headers
    Gateway->>CoreSvc: Forward request
    CoreSvc-->>Client: Return order list
```

### 🚶 Step-by-Step Flow:
1. **Logging In**: You send your credentials (username/password) to the API Gateway. The Gateway forwards them to the **User Service**, which verifies them against the PostgreSQL database.
2. **Receiving a Token**: If details match, the User Service signs and generates a **JWT (JSON Web Token)** and returns it to you.
3. **Accessing Secured Features**: For all future requests, you include this token. The **API Gateway** checks the token's signature. If valid, it extracts your user ID and forwards the request to downstream services (like the Order Service).

---

## 2. Order Placement & Transactional Outbox Pattern

### 💡 The Analogy: The Diary & The Postman
When you order food, two things must happen:
1. The order must be saved in our system (**PostgreSQL Database**).
2. A notification must be sent to the kitchen and drivers (**Kafka Broker**).

If we save the order but the network fails before notifying the kitchen, the customer gets charged for food that is never cooked. If we notify the kitchen but the database fails, the kitchen cooks food that doesn't exist in our system.

To solve this, we use the **Outbox Pattern**:
* You write the order details in your **Order Book** AND write a draft letter in your **Outbox Drafts Folder** (both in PostgreSQL) at the *exact same time*. If you drop your pen, both are discarded (rolled back).
* A postman (**Outbox Scheduler**) checks the Outbox folder every 2 seconds. When he finds a draft, he delivers it to the post office (**Kafka**) and marks the draft as "Sent" in the book.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Customer
    participant OrderSvc as Order Writer (Order Service)
    participant RestSvc as Restaurant Checker (Restaurant Service)
    participant Database as Diary & Outbox (PostgreSQL)
    participant Scheduler as Postman (Outbox Scheduler)
    participant Kafka as Post Office (Kafka Broker)

    Client->>OrderSvc: 1. Place order for Margherita Pizza
    OrderSvc->>RestSvc: Is Pizza on the menu and price correct?
    RestSvc-->>OrderSvc: Yes, verified!
    
    rect rgb(230, 245, 255)
        Note over OrderSvc, Database: Lock-Step Transaction (Both or Nothing)
        OrderSvc->>Database: Write Order (Status: CREATED)
        OrderSvc->>Database: Write draft event in Outbox table
    end
    
    Database-->>OrderSvc: Saved successfully!
    OrderSvc-->>Client: Return Order ID (Success)

    loop Every 2 seconds
        Scheduler->>Database: Look for unsent drafts
        Database-->>Scheduler: Found unsent draft
        Scheduler->>Kafka: Deliver message to topic 'order-events'
        Kafka-->>Scheduler: Acknowledged!
        Scheduler->>Database: Mark draft as "Sent" (processed = true)
    end
```

### 🚶 Step-by-Step Flow:
1. **Validation**: The **Order Service** verifies with the **Restaurant Service** that the item exists and the price is correct.
2. **Atomicity**: The Order Service writes the order to the `orders` table AND a message to the `outbox_events` table inside a single database transaction.
3. **Event Delivery**: The [OutboxScheduler](file:///e:/Learning/antigravity-projects/springboot-app/order-service/src/main/java/com/quickeats/orderservice/scheduler/OutboxScheduler.java) scans the database, finds the unsent event, publishes it to Kafka, and marks it as processed.

---

## 3. Order Saga & Payment Orchestration

### 💡 The Analogy: Booking a Vacation
When booking a trip, you book a hotel first, then a flight. If the flight booking fails, you must cancel the hotel booking to get your money back. This is called a **Saga**.

```mermaid
stateDiagram-v2
    [*] --> CREATED : Place Order
    
    state CREATED {
        [*] --> Outbox_Drafted
        Outbox_Drafted --> Kafka_Published : Postman sends to Kafka
    }
    
    CREATED --> PENDING_PAYMENT : Payment Service hears event
    
    state PENDING_PAYMENT {
        [*] --> Waiting_For_Charge
        Waiting_For_Charge --> PAID : Customer pays (Success)
        Waiting_For_Charge --> PAYMENT_FAILED : Card declined (Failure)
    }
    
    PAID --> CONFIRMED : Order Service updates order to CONFIRMED
    PAYMENT_FAILED --> CANCELLED : Order Service cancels order (Compensating action)
    
    CONFIRMED --> [*]
    CANCELLED --> [*]
```

### 🚶 Step-by-Step Flow:
1. **Order Initialized**: The order starts as `CREATED`.
2. **Payment Listens**: The **Payment Service** listens to Kafka and sets up a `PENDING` transaction.
3. **Payment Collection**:
   * **If Payment Succeeds**: The payment status changes to `PAID`. A Kafka message is sent, and the **Order Service** updates the order status to `CONFIRMED`.
   * **If Payment Fails**: A `PAYMENT_FAILED` event is published. The **Order Service** catches this event and performs a **compensating transaction** (automatically transitioning the order status to `CANCELLED` so you aren't stuck with an unpaid order).

---

## 4. Proximity Matching via Redis Geo

### 💡 The Analogy: The Digital Radar Screen
Imagine a radar drawing a circle around a restaurant to find the nearest delivery driver.
1. Drivers constantly pin their GPS coordinates on our radar map (**Redis Geo Index**).
2. When an order is ready, we set the restaurant as the center point.
3. We scan a 3km radius to find the closest active driver's dot.

```mermaid
sequenceDiagram
    autonumber
    actor Driver as Delivery Partner
    participant DelivSvc as Radar Controller (Delivery Service)
    participant Redis as Radar Map (Redis Geo Cache)
    participant Postgres as Filing Cabinet (PostgreSQL)
    participant Kafka as Event Queue

    Driver->>DelivSvc: 1. Send GPS: I'm active at MG Road (lat, lon)
    DelivSvc->>Redis: Pin location (GEOADD)
    Redis-->>DelivSvc: Pinned!

    Kafka->>DelivSvc: 2. Order PAID event received (Restaurant Location)
    DelivSvc->>Redis: Search within 3km of Restaurant (GEORADIUS)
    Redis-->>DelivSvc: Closest driver is "driver-guy-1" (500m away)
    
    DelivSvc->>Redis: Erase driver from active pool (ZREM - avoid double-booking)
    DelivSvc->>Postgres: Record run details (Status: ASSIGNED)
    DelivSvc->>Kafka: Publish "Delivery Assigned" event
```

### 🚶 Step-by-Step Flow:
1. **Active Drivers**: Drivers register their locations in Redis using the `GEOADD` command under `delivery:partners:active`.
2. **Proximity Search**: Upon order payment, the **Delivery Service** runs a `GEORADIUS` query centering on the restaurant coordinates.
3. **Locking the Driver**: The nearest driver is selected, and their ID is removed (`ZREM`) from active search pool so they aren't assigned multiple orders simultaneously.

---

## 5. Real-Time Telemetry & STOMP WebSockets Tracking

### 💡 The Analogy: A Continuous Phone Call
In old web apps, to see where your food is, the browser had to ask the server "where is the driver now?" every few seconds (**Polling**). This wasted energy.
With **WebSockets (STOMP)**, it is like a phone call that stays open. Once the call is connected, the server continuously whispers the driver's coordinates to the client without the client asking.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Customer App
    participant Gateway as API Gateway
    participant DelivSvc as Delivery Service
    participant Database as Location History (PostgreSQL)
    actor Driver as Delivery Partner

    Client->>Gateway: 1. Open persistent WebSockets call (ws://localhost:8080/ws-delivery)
    Gateway->>DelivSvc: Bridge connection to service
    Client->>DelivSvc: Listen to channel "/topic/delivery/order-1"

    loop As driver moves towards customer
        Driver->>DelivSvc: 2. Send location update (lat, lon)
        DelivSvc->>Database: Write coordinates log
        DelivSvc->>Client: Instantly stream location to listener channel
    end

    Driver->>DelivSvc: 3. Completed Delivery
    DelivSvc->>Database: Save status as DELIVERED
    DelivSvc->>Client: Send final update (Close tracking UI)
```

### 🚶 Step-by-Step Flow:
1. **Establish Channel**: The customer's browser initiates a WebSocket connection through the **API Gateway** to the **Delivery Service** and subscribes to `/topic/delivery/{orderId}`.
2. **Telemetry Updates**: As the driver drives, their app sends GPS coordinate packets (`POST /deliveries/{id}/location`).
3. **Broadcasting**: The Delivery Service saves the location in PostgreSQL and broadcasts the coordinates over the active WebSocket channel. The customer's map updates in real time.
