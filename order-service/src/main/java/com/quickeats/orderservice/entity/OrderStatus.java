package com.quickeats.orderservice.entity;

public enum OrderStatus {
    CREATED,
    PENDING_PAYMENT,
    PAID,
    CANCELLED,
    REFUNDED
}
