package com.quickeats.deliveryservice.service.impl;

import com.quickeats.deliveryservice.dto.RestaurantResponse;
import com.quickeats.deliveryservice.entity.Delivery;
import com.quickeats.deliveryservice.entity.DeliveryStatus;
import com.quickeats.deliveryservice.repository.DeliveryRepository;
import com.quickeats.deliveryservice.service.DeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Qualifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class DeliveryServiceImpl implements DeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryServiceImpl.class);
    private static final String PARTNERS_GEO_KEY = "delivery:partners:active";

    private final DeliveryRepository deliveryRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;

    public DeliveryServiceImpl(DeliveryRepository deliveryRepository,
            @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
            SimpMessagingTemplate messagingTemplate,
            RestTemplate restTemplate) {
        this.deliveryRepository = deliveryRepository;
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.restTemplate = restTemplate;
    }

    @Override
    @Transactional
    public Delivery matchDeliveryPartner(Long orderId, String restaurantId, BigDecimal totalAmount) {
        logger.info("Starting delivery partner matching for order ID: {}, restaurant ID: {}", orderId, restaurantId);

        // 1. Fetch restaurant location from restaurant-service
        String restaurantUrl = "http://restaurant-service/api/v1/restaurants/" + restaurantId;
        double restaurantLng = 77.5946; // Fallback longitude
        double restaurantLat = 12.9716; // Fallback latitude

        try {
            RestaurantResponse restaurant = restTemplate.getForObject(restaurantUrl, RestaurantResponse.class);
            if (restaurant != null && restaurant.getLocation() != null && restaurant.getLocation().length >= 2) {
                restaurantLng = restaurant.getLocation()[0];
                restaurantLat = restaurant.getLocation()[1];
                logger.info("Fetched restaurant location: [lng: {}, lat: {}]", restaurantLng, restaurantLat);
            } else {
                logger.warn("Restaurant or restaurant location was null. Using fallback coordinates.");
            }
        } catch (Exception e) {
            logger.error(
                    "Failed to fetch restaurant location from restaurant-service. Using fallback coordinates. Error: {}",
                    e.getMessage());
        }

        // 2. Query Redis Geo Set for the nearest active partner within 5km
        Circle circle = new Circle(new Point(restaurantLng, restaurantLat), new Distance(5.0, Metrics.KILOMETERS));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance()
                .sortAscending()
                .limit(1);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()
                .radius(PARTNERS_GEO_KEY, circle, args);

        String partnerId;
        if (results != null && !results.getContent().isEmpty()) {
            partnerId = results.getContent().iterator().next().getContent().getName();
            double distance = results.getContent().iterator().next().getDistance().getValue();
            logger.info("Found nearest delivery partner: {} at a distance of {} km", partnerId, distance);
        } else {
            logger.warn("No active delivery partners found within 5km. Assigning fallback-partner-1.");
            partnerId = "fallback-partner-1";
        }

        // 3. Create persistent delivery record in DB
        Delivery delivery = new Delivery();
        delivery.setOrderId(orderId);
        delivery.setPartnerId(partnerId);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setTotalAmount(totalAmount);
        delivery = deliveryRepository.save(delivery);

        // 4. Remove partner from the active pool to mark them as busy
        if (!"fallback-partner-1".equals(partnerId)) {
            redisTemplate.opsForZSet().remove(PARTNERS_GEO_KEY, partnerId);
            logger.info("Removed delivery partner: {} from the active pool", partnerId);
        }

        // 5. Broadcast assignment state to WebSocket clients
        broadcastLocationUpdate(delivery, restaurantLat, restaurantLng);

        return delivery;
    }

    @SuppressWarnings("null")
    @Override
    public void registerActivePartner(String partnerId, double latitude, double longitude) {
        logger.info("Registering/Updating active partner: {} at [lat: {}, lng: {}]", partnerId, latitude, longitude);
        // Redis expects X (longitude) and Y (latitude)
        redisTemplate.opsForGeo().add(PARTNERS_GEO_KEY, new Point(longitude, latitude), partnerId);
    }

    @Override
    @Transactional
    public void updateLocation(Long deliveryId, double latitude, double longitude, String status) {
        logger.info("Updating location for delivery ID: {} to [lat: {}, lng: {}], status: {}", deliveryId, latitude,
                longitude, status);

        @SuppressWarnings("null")
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found with ID: " + deliveryId));

        if (status != null && !status.isEmpty()) {
            try {
                DeliveryStatus newStatus = DeliveryStatus.valueOf(status.toUpperCase());
                delivery.setStatus(newStatus);
                delivery = deliveryRepository.save(delivery);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid status provided for delivery location update: {}", status);
            }
        }

        broadcastLocationUpdate(delivery, latitude, longitude);
    }

    @Override
    @Transactional
    public void completeDelivery(Long deliveryId) {
        logger.info("Completing delivery run for ID: {}", deliveryId);

        @SuppressWarnings("null")
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found with ID: " + deliveryId));

        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery = deliveryRepository.save(delivery);

        // Broadcast complete status to STOMP subscribers (we can pass 0,0 or last known
        // coordinates if we had them)
        broadcastLocationUpdate(delivery, 0.0, 0.0);
    }

    @SuppressWarnings("null")
    private void broadcastLocationUpdate(Delivery delivery, double latitude, double longitude) {
        String destination = "/topic/delivery/" + delivery.getOrderId();
        Map<String, Object> payload = Map.of(
                "deliveryId", delivery.getId(),
                "orderId", delivery.getOrderId(),
                "partnerId", delivery.getPartnerId() != null ? delivery.getPartnerId() : "",
                "status", delivery.getStatus().name(),
                "latitude", latitude,
                "longitude", longitude,
                "timestamp", LocalDateTime.now().toString());
        logger.info("Broadcasting location/status update to STOMP destination {}: {}", destination, payload);
        messagingTemplate.convertAndSend(destination, payload);
    }
}
