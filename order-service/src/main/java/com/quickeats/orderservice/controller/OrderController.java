package com.quickeats.orderservice.controller;

import com.quickeats.orderservice.dto.OrderRequest;
import com.quickeats.orderservice.dto.OrderResponse;
import com.quickeats.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @Valid @RequestBody OrderRequest request,
            @RequestHeader("X-User-Id") String userIdHeader) {
        Long userId = Long.parseLong(userIdHeader);
        return orderService.createOrder(request, userId);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable("id") Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping
    public List<OrderResponse> getOrdersByUserId(@RequestHeader("X-User-Id") String userIdHeader) {
        Long userId = Long.parseLong(userIdHeader);
        return orderService.getOrdersByUserId(userId);
    }

    @PutMapping("/{id}/cancel")
    public OrderResponse cancelOrder(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRoleHeader) {
        Long userId = Long.parseLong(userIdHeader);
        return orderService.cancelOrder(id, userId, userRoleHeader);
    }
}
