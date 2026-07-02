package com.quickeats.notificationservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickeats.notificationservice.dto.OrderEvent;
import com.quickeats.notificationservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);

    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final String toEmail;

    public OrderEventListener(
            EmailService emailService,
            SimpMessagingTemplate messagingTemplate,
            @Value("${resend.to-email:delivered@resend.dev}") String toEmail) {
        this.emailService = emailService;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper();
        this.toEmail = toEmail;
    }

    @KafkaListener(topics = "order-events", groupId = "notification-group")
    public void handleOrderEvent(String message) {
        logger.info("Received Kafka Order Event message: {}", message);

        try {
            OrderEvent event = objectMapper.readValue(message, OrderEvent.class);
            logger.info("Decoded Order Event - ID: {}, Status: {}, Total: {}", event.getOrderId(), event.getStatus(), event.getTotalAmount());

            String subject;
            String bodyHtml;

            switch (event.getStatus().toUpperCase()) {
                case "PAID":
                    subject = "QuickEats - Order Received (#" + event.getOrderId() + ")";
                    bodyHtml = String.format(
                            "<h3>Order Confirmed!</h3>" +
                            "<p>We have successfully processed your payment of <strong>INR %s</strong> for Order <strong>#%s</strong>.</p>" +
                            "<p>Your order has been sent to the kitchen and is now being prepared. Thank you for choosing QuickEats!</p>",
                            event.getTotalAmount(), event.getOrderId()
                    );
                    break;

                case "CANCELLED":
                    subject = "QuickEats - Order Cancelled (#" + event.getOrderId() + ")";
                    bodyHtml = String.format(
                            "<h3>Order Cancelled</h3>" +
                            "<p>Your order <strong>#%s</strong> has been cancelled.</p>" +
                            "<p>If any payment was deducted, a refund will be initiated automatically. We hope to serve you again soon.</p>",
                            event.getOrderId()
                    );
                    break;

                case "REFUNDED":
                    subject = "QuickEats - Refund Processed (#" + event.getOrderId() + ")";
                    bodyHtml = String.format(
                            "<h3>Refund Processed</h3>" +
                            "<p>A refund of <strong>INR %s</strong> for your cancelled Order <strong>#%s</strong> has been successfully credited back to your account.</p>" +
                            "<p>It may take 2-3 business days to reflect in your bank statement.</p>",
                            event.getTotalAmount(), event.getOrderId()
                    );
                    break;

                default:
                    logger.info("Ignoring event with status: {}", event.getStatus());
                    return;
            }

            emailService.sendEmail(toEmail, subject, bodyHtml);

            String destination = "/topic/notifications/" + event.getUserId();
            logger.info("Publishing real-time WebSocket notification to {}: {}", destination, event);
            messagingTemplate.convertAndSend(destination, event);

        } catch (Exception e) {
            logger.error("Error processing incoming Kafka Order Event message: {}", e.getMessage(), e);
        }
    }
}
