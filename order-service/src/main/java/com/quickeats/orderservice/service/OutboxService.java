package com.quickeats.orderservice.service;

import com.quickeats.orderservice.entity.Order;

public interface OutboxService {
    void saveOrderEvent(Order order, String eventType);
}
