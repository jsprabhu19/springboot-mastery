package com.quickeats.orderservice.entity;

import jakarta.persistence.*;
import org.springframework.lang.NonNull;
import java.time.ZonedDateTime;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @NonNull
    private String id = "";

    @Column(name = "aggregate_type", nullable = false)
    @NonNull
    private String aggregateType = "";

    @Column(name = "aggregate_id", nullable = false)
    @NonNull
    private String aggregateId = "";

    @Column(name = "event_type", nullable = false)
    @NonNull
    private String eventType = "";

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    @NonNull
    private String payload = "";

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "processed")
    private boolean processed;

    @PrePersist
    protected void onCreate() {
        createdAt = ZonedDateTime.now();
        processed = false;
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(@NonNull String aggregateType) { this.aggregateType = aggregateType; }

    @NonNull
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(@NonNull String aggregateId) { this.aggregateId = aggregateId; }

    @NonNull
    public String getEventType() { return eventType; }
    public void setEventType(@NonNull String eventType) { this.eventType = eventType; }

    @NonNull
    public String getPayload() { return payload; }
    public void setPayload(@NonNull String payload) { this.payload = payload; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }
}
