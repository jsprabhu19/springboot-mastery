package com.quickeats.orderservice.service.impl;

import com.quickeats.orderservice.dto.OrderRequest;
import com.quickeats.orderservice.dto.OrderResponse;
import com.quickeats.orderservice.entity.Order;
import com.quickeats.orderservice.entity.OrderItem;
import com.quickeats.orderservice.entity.OrderStatus;
import com.quickeats.orderservice.exception.OrderException;
import com.quickeats.orderservice.repository.OrderRepository;
import com.quickeats.orderservice.service.OrderService;
import com.quickeats.orderservice.service.OrderSagaOrchestrator;
import com.quickeats.orderservice.service.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator sagaOrchestrator;
    private final OutboxService outboxService;

    public OrderServiceImpl(OrderRepository orderRepository, OrderSagaOrchestrator sagaOrchestrator, OutboxService outboxService) {
        this.orderRepository = orderRepository;
        this.sagaOrchestrator = sagaOrchestrator;
        this.outboxService = outboxService;
    }

    @Override
    public OrderResponse createOrder(OrderRequest request, Long userId) {
        logger.info("Creating new order for User: {}, Restaurant: {}", userId, request.getRestaurantId());

        // Calculate total amount
        BigDecimal total = request.getItems().stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 1. Create order skeleton in status CREATED
        Order order = new Order();
        order.setUserId(userId);
        order.setRestaurantId(request.getRestaurantId());
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(total);

        // Add items
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setName(itemRequest.getName());
            item.setPrice(itemRequest.getPrice());
            item.setQuantity(itemRequest.getQuantity());
            order.addItem(item);
        }

        // Save order to generate primary ID
        order = orderRepository.saveAndFlush(order);

        // 2. Trigger the REST Saga Orchestrator to validate and process payment
        sagaOrchestrator.executeSaga(order, request);

        // Refresh database state
        order = orderRepository.findById(order.getId()).orElse(order);
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderException("Order not found with ID: " + id, HttpStatus.NOT_FOUND));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id, Long userId, String role) {
        logger.info("Processing cancellation request for Order ID: {} by User: {} with Role: {}", id, userId, role);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderException("Order not found with ID: " + id, HttpStatus.NOT_FOUND));

        // Security check: Only the order owner or an ADMIN can cancel the order
        if (!order.getUserId().equals(userId) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new OrderException("Unauthorized to cancel this order", HttpStatus.FORBIDDEN);
        }

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED) {
            logger.warn("Order: {} is already terminated with status: {}", id, order.getStatus());
            return mapToResponse(order);
        }

        // If order is paid, trigger compensating refund transaction
        if (order.getStatus() == OrderStatus.PAID) {
            sagaOrchestrator.executeRefundSaga(order);
        } else {
            // Just cancel directly if not yet paid
            order.setStatus(OrderStatus.CANCELLED);
            order = orderRepository.save(order);
            outboxService.saveOrderEvent(order, "CANCELLED");
        }

        return mapToResponse(order);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderResponse.OrderItemResponse(
                        item.getId(),
                        item.getName(),
                        item.getPrice(),
                        item.getQuantity()
                ))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getRestaurantId(),
                order.getStatus(),
                order.getTotalAmount(),
                itemResponses,
                order.getCreatedAt()
        );
    }
}
