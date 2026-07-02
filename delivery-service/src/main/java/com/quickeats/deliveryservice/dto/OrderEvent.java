package com.quickeats.deliveryservice.dto;

import java.math.BigDecimal;

public class OrderEvent {
    private Long orderId;
    private Long userId;
    private String restaurantId;
    private String status;
    private BigDecimal totalAmount;

    public OrderEvent() {}

    public OrderEvent(Long orderId, Long userId, String restaurantId, String status, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
