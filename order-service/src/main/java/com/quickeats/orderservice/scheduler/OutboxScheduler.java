package com.quickeats.orderservice.scheduler;

import com.quickeats.orderservice.entity.OutboxEvent;
import com.quickeats.orderservice.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxScheduler {

    private static final Logger logger = LoggerFactory.getLogger(OutboxScheduler.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxScheduler(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 2000)
    public void processOutboxEvents() {
        List<OutboxEvent> unprocessedEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        if (unprocessedEvents.isEmpty()) {
            return;
        }

        logger.info("Found {} unprocessed outbox events to publish", unprocessedEvents.size());

        for (OutboxEvent event : unprocessedEvents) {
            try {
                logger.info("Publishing event ID: {} (Type: {}, AggregateId: {}) to Kafka",
                        event.getId(), event.getEventType(), event.getAggregateId());

                // Publish to Kafka and block for result (ensures at-least-once delivery guarantee)
                kafkaTemplate.send("order-events", event.getAggregateId(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);

                // Mark as processed only after successful Kafka publish
                event.setProcessed(true);
                outboxEventRepository.saveAndFlush(event);
                logger.info("Successfully published and marked outbox event ID: {} as processed", event.getId());

            } catch (Exception e) {
                logger.error("Failed to publish outbox event ID: {} to Kafka. Will retry next cycle. Error: {}",
                        event.getId(), e.getMessage());
                // Break to preserve order of events and avoid repeatedly failing on other events during network drop
                break;
            }
        }
    }
}
