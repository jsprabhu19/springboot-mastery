package com.quickeats.orderservice.service;

import com.quickeats.orderservice.dto.OrderRequest;
import com.quickeats.orderservice.dto.OrderResponse;
import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request, Long userId);
    OrderResponse getOrderById(Long id);
    List<OrderResponse> getOrdersByUserId(Long userId);
    OrderResponse cancelOrder(Long id, Long userId, String role);
}
