package com.quickeats.orderservice.service;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.quickeats.orderservice.dto.MenuItemResponse;
import com.quickeats.orderservice.dto.OrderRequest;
import com.quickeats.orderservice.dto.PaymentRequest;
import com.quickeats.orderservice.dto.PaymentResponse;
import com.quickeats.orderservice.dto.RestaurantResponse;
import com.quickeats.orderservice.entity.Order;
import com.quickeats.orderservice.entity.OrderStatus;
import com.quickeats.orderservice.exception.OrderException;
import com.quickeats.orderservice.repository.OrderRepository;

@Component
public class OrderSagaOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;
    private final OutboxService outboxService;

    // Self-injection for @Transactional proxy invocation (cannot use constructor
    // injection here)
    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private OrderSagaOrchestrator self;

    public OrderSagaOrchestrator(RestTemplate restTemplate, OrderRepository orderRepository,
            OutboxService outboxService) {
        this.restTemplate = restTemplate;
        this.orderRepository = orderRepository;
        this.outboxService = outboxService;
    }

    /**
     * Executes the Order Creation + Payment Saga workflow synchronously.
     */
    public void executeSaga(Order order, OrderRequest request) {
        logger.info("Starting Saga Orchestrator for Order ID: {}", order.getId());

        // Step 1: Validate restaurant menu and items
        try {
            validateRestaurantItems(order.getRestaurantId(), request);
        } catch (Exception e) {
            logger.error("Saga Step 1 (Restaurant Validation) failed for Order: {}. Reason: {}", order.getId(),
                    e.getMessage());
            self.updateOrderStatusAndSaveEvent(order.getId(), OrderStatus.CANCELLED);
            throw new OrderException("Order validation failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // Step 2: Transition order to PENDING_PAYMENT
        self.updateOrderStatusAndSaveEvent(order.getId(), OrderStatus.PENDING_PAYMENT);
        logger.info("Saga Step 1 successful. Order ID: {} transitioned to PENDING_PAYMENT", order.getId());

        // Step 3: Process payment via payment-service
        String idempotencyKey = "order-idempotency-" + order.getId() + "-"
                + UUID.randomUUID().toString().substring(0, 8);
        try {
            processPayment(order, idempotencyKey);
        } catch (Exception e) {
            logger.error("Saga Step 3 (Payment Process) failed for Order: {}. Initiating compensating rollback.",
                    order.getId());
            self.updateOrderStatusAndSaveEvent(order.getId(), OrderStatus.CANCELLED);
            throw new OrderException("Payment processing failed: " + e.getMessage(), HttpStatus.PAYMENT_REQUIRED);
        }

        // Step 4: Finalize order status to PAID
        self.updateOrderStatusAndSaveEvent(order.getId(), OrderStatus.PAID);
        logger.info("Saga Orchestrator completed successfully for Order ID: {}. Status is PAID.", order.getId());
    }

    /**
     * Executes compensating rollback to refund a paid order and cancel it.
     */
    @SuppressWarnings("null")
    public void executeRefundSaga(Order order) {
        logger.info("Initiating Compensating Refund Saga for Order ID: {}", order.getId());

        if (order.getStatus() != OrderStatus.PAID) {
            throw new OrderException(
                    "Only orders with status PAID can be refunded. Current status: " + order.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        try {
            // Call payment-service refund
            String refundUrl = "http://payment-service/api/v1/payments/refund?orderId=" + order.getId();

            HttpHeaders headers = createHeadersWithContext();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            restTemplate.postForObject(refundUrl, entity, PaymentResponse.class);
            logger.info("Refund request processed successfully by Payment Service for Order: {}", order.getId());

            self.updateOrderStatusAndSaveEvent(order.getId(), OrderStatus.REFUNDED);
        } catch (Exception e) {
            logger.error(
                    "Failed to execute compensating refund saga for Order ID: {}. System requires manual reconciliation. Error: {}",
                    order.getId(), e.getMessage());
            throw new OrderException("Compensating refund transaction failed: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @SuppressWarnings("null")
    @Transactional
    public void updateOrderStatusAndSaveEvent(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException("Order not found with ID: " + orderId, HttpStatus.NOT_FOUND));
        order.setStatus(status);
        orderRepository.saveAndFlush(order);

        // Write terminal state transitions to the Outbox
        if (status == OrderStatus.PAID || status == OrderStatus.CANCELLED || status == OrderStatus.REFUNDED) {
            outboxService.saveOrderEvent(order, status.name());
        }
    }

    private HttpHeaders createHeadersWithContext() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        org.springframework.web.context.request.RequestAttributes attributes = org.springframework.web.context.request.RequestContextHolder
                .getRequestAttributes();
        if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servletRequestAttributes) {
            jakarta.servlet.http.HttpServletRequest currentRequest = servletRequestAttributes.getRequest();
            String userId = currentRequest.getHeader("X-User-Id");
            String userRole = currentRequest.getHeader("X-User-Role");
            String userName = currentRequest.getHeader("X-User-Name");

            if (userId != null)
                headers.set("X-User-Id", userId);
            if (userRole != null)
                headers.set("X-User-Role", userRole);
            if (userName != null)
                headers.set("X-User-Name", userName);
        }
        return headers;
    }

    @SuppressWarnings("null")
    private void validateRestaurantItems(String restaurantId, OrderRequest request) {
        String restaurantUrl = "http://restaurant-service/api/v1/restaurants/" + restaurantId;
        logger.info("Calling restaurant-service: {} to validate items", restaurantUrl);

        RestaurantResponse restaurant;
        try {
            restaurant = restTemplate.getForObject(restaurantUrl, RestaurantResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Restaurant not found in restaurant directory.");
        } catch (Exception e) {
            throw new RuntimeException("Unable to contact restaurant-service: " + e.getMessage());
        }

        if (restaurant == null) {
            throw new RuntimeException("Restaurant response was empty.");
        }

        // Map menu items by name for verification
        Map<String, MenuItemResponse> menuMap = restaurant.getMenuItems().stream()
                .collect(Collectors.toMap(MenuItemResponse::getName, item -> item, (item1, item2) -> item1));

        // Validate each item
        for (OrderRequest.OrderItemRequest orderItem : request.getItems()) {
            MenuItemResponse menuItem = menuMap.get(orderItem.getName());
            if (menuItem == null) {
                throw new RuntimeException(
                        String.format("Menu item '%s' is not offered by this restaurant.", orderItem.getName()));
            }
            if (!menuItem.isAvailable()) {
                throw new RuntimeException(
                        String.format("Menu item '%s' is currently out of stock.", orderItem.getName()));
            }
            // Check price accuracy
            if (menuItem.getPrice().compareTo(orderItem.getPrice()) != 0) {
                throw new RuntimeException(String.format("Price mismatch for item '%s'. Expected: %s, Received: %s",
                        orderItem.getName(), menuItem.getPrice(), orderItem.getPrice()));
            }
        }
    }

    private void processPayment(Order order, String idempotencyKey) {
        String paymentUrl = "http://payment-service/api/v1/payments";
        logger.info("Calling payment-service: {} to charge amount: {}", paymentUrl, order.getTotalAmount());

        PaymentRequest request = new PaymentRequest(
                order.getId().toString(),
                order.getTotalAmount(),
                idempotencyKey);

        HttpHeaders headers = createHeadersWithContext();
        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);

        try {
            PaymentResponse response = restTemplate.postForObject(paymentUrl, entity, PaymentResponse.class);
            if (response == null || !"SUCCESS".equalsIgnoreCase(response.getStatus())) {
                throw new RuntimeException(
                        response != null ? "Payment status: " + response.getStatus() : "Null response from gateway");
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Payment service rejected transaction: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to payment-service: " + e.getMessage());
        }
    }
}
