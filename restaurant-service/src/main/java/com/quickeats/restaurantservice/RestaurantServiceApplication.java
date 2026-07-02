package com.quickeats.restaurantservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Restaurant Service managing menu details and locations.
 * Backed by MongoDB and uses Redis for high-read menu caching.
 */
@SpringBootApplication
@EnableCaching
public class RestaurantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RestaurantServiceApplication.class, args);
    }
}
