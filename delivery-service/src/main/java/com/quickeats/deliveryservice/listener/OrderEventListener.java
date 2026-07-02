package com.quickeats.deliveryservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickeats.deliveryservice.dto.OrderEvent;
import com.quickeats.deliveryservice.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);
    private final DeliveryService deliveryService;
    private final ObjectMapper objectMapper;

    public OrderEventListener(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "order-events", groupId = "delivery-group")
    public void handleOrderEvent(String message) {
        logger.info("Received Kafka Order Event message in delivery-service: {}", message);

        try {
            OrderEvent event = objectMapper.readValue(message, OrderEvent.class);
            logger.info("Decoded Order Event in delivery-service - ID: {}, Status: {}, Restaurant: {}", 
                    event.getOrderId(), event.getStatus(), event.getRestaurantId());

            if ("PAID".equalsIgnoreCase(event.getStatus())) {
                logger.info("Order status is PAID. Triggering proximity-based delivery matching for order ID: {}", event.getOrderId());
                deliveryService.matchDeliveryPartner(
                        event.getOrderId(),
                        event.getRestaurantId(),
                        event.getTotalAmount()
                );
            } else {
                logger.info("Ignoring order status: {} for delivery matching", event.getStatus());
            }

        } catch (Exception e) {
            logger.error("Error processing incoming Kafka Order Event message in delivery-service: {}", e.getMessage(), e);
        }
    }
}
