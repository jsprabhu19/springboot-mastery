package com.quickeats.deliveryservice.controller;

import com.quickeats.deliveryservice.dto.DeliveryResponse;
import com.quickeats.deliveryservice.dto.LocationUpdate;
import com.quickeats.deliveryservice.dto.PartnerRegistration;
import com.quickeats.deliveryservice.entity.Delivery;
import com.quickeats.deliveryservice.repository.DeliveryRepository;
import com.quickeats.deliveryservice.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final DeliveryRepository deliveryRepository;

    public DeliveryController(DeliveryService deliveryService, DeliveryRepository deliveryRepository) {
        this.deliveryService = deliveryService;
        this.deliveryRepository = deliveryRepository;
    }

    @PostMapping("/partners/active")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerActivePartner(@Valid @RequestBody PartnerRegistration registration) {
        deliveryService.registerActivePartner(
                registration.getPartnerId(),
                registration.getLatitude(),
                registration.getLongitude());
    }

    @PostMapping("/{id}/location")
    public void updateLocation(@PathVariable("id") Long id, @Valid @RequestBody LocationUpdate update) {
        deliveryService.updateLocation(
                id,
                update.getLatitude(),
                update.getLongitude(),
                update.getStatus());
    }

    @PostMapping("/{id}/complete")
    public void completeDelivery(@PathVariable("id") Long id) {
        deliveryService.completeDelivery(id);
    }

    @GetMapping("/{id}")
    public DeliveryResponse getDeliveryById(@PathVariable("id") @NonNull Long id) {
        Delivery delivery = deliveryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery not found with ID: " + id));
        return mapToResponse(delivery);
    }

    @GetMapping("/order/{orderId}")
    public DeliveryResponse getDeliveryByOrderId(@PathVariable("orderId") Long orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery not found for order ID: " + orderId));
        return mapToResponse(delivery);
    }

    private DeliveryResponse mapToResponse(Delivery delivery) {
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getOrderId(),
                delivery.getPartnerId(),
                delivery.getStatus(),
                delivery.getTotalAmount(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt());
    }
}
