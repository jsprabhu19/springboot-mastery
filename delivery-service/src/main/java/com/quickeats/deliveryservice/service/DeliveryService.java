package com.quickeats.deliveryservice.service;

import com.quickeats.deliveryservice.entity.Delivery;

import java.math.BigDecimal;

public interface DeliveryService {
    Delivery matchDeliveryPartner(Long orderId, String restaurantId, BigDecimal totalAmount);
    void registerActivePartner(String partnerId, double latitude, double longitude);
    void updateLocation(Long deliveryId, double latitude, double longitude, String status);
    void completeDelivery(Long deliveryId);
}
