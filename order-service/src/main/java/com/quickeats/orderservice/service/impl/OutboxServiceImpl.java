package com.quickeats.orderservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickeats.orderservice.entity.Order;
import com.quickeats.orderservice.entity.OutboxEvent;
import com.quickeats.orderservice.repository.OutboxEventRepository;
import com.quickeats.orderservice.service.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OutboxServiceImpl implements OutboxService {

    private static final Logger logger = LoggerFactory.getLogger(OutboxServiceImpl.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxServiceImpl(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = new ObjectMapper();
    }

    @SuppressWarnings("null")
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveOrderEvent(Order order, String eventType) {
        logger.info("Persisting outbox event for Order ID: {} - Status: {}", order.getId(), eventType);

        try {
            // Build simple payload map
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("orderId", order.getId());
            payloadMap.put("userId", order.getUserId());
            payloadMap.put("restaurantId", order.getRestaurantId());
            payloadMap.put("status", eventType);
            payloadMap.put("totalAmount", order.getTotalAmount());

            String jsonPayload = objectMapper.writeValueAsString(payloadMap);

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setId(UUID.randomUUID().toString());
            outboxEvent.setAggregateType("ORDER");
            outboxEvent.setAggregateId(order.getId().toString());
            outboxEvent.setEventType(eventType);
            outboxEvent.setPayload(jsonPayload);

            outboxEventRepository.save(outboxEvent);
            logger.info("Successfully persisted outbox event ID: {}", outboxEvent.getId());
        } catch (Exception e) {
            logger.error("Failed to serialize outbox event payload: {}", e.getMessage(), e);
            throw new RuntimeException("Outbox serialization failure", e);
        }
    }
}
