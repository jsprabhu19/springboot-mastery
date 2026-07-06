package com.quickeats.restaurantservice.service.impl;

import com.quickeats.restaurantservice.document.MenuItem;
import com.quickeats.restaurantservice.document.Restaurant;
import com.quickeats.restaurantservice.dto.MenuItemRequest;
import com.quickeats.restaurantservice.dto.RestaurantRequest;
import com.quickeats.restaurantservice.exception.RestaurantException;
import com.quickeats.restaurantservice.repository.RestaurantRepository;
import com.quickeats.restaurantservice.service.RestaurantService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of RestaurantService.
 * Configured with Redis Caching annotations to speed up read access.
 * Explicitly evicts stale cache elements on writes to avoid dirty reads.
 */
@Service
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;

    public RestaurantServiceImpl(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    @SuppressWarnings("null")
    @Override
    public Restaurant createRestaurant(RestaurantRequest request) {
        List<MenuItem> items = request.getMenuItems().stream()
                .map(item -> new MenuItem(
                        UUID.randomUUID().toString(),
                        item.getName(),
                        item.getDescription(),
                        item.getPrice(),
                        item.isAvailable()))
                .toList();

        Restaurant restaurant = Restaurant.builder()
                .name(request.getName())
                .cuisineType(request.getCuisineType())
                .address(request.getAddress())
                .location(request.getLongitude(), request.getLatitude())
                .menuItems(items)
                .build();

        return restaurantRepository.save(restaurant);
    }

    @SuppressWarnings("null")
    @Override
    @Cacheable(value = "restaurants", key = "#p0")
    public Restaurant getRestaurantById(String id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new RestaurantException("Restaurant not found", HttpStatus.NOT_FOUND));
    }

    @Override
    @Cacheable(value = "menus", key = "#p0")
    public List<MenuItem> getRestaurantMenu(String restaurantId) {
        Restaurant restaurant = getRestaurantById(restaurantId);
        return restaurant.getMenuItems();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "restaurants", key = "#p0"),
            @CacheEvict(value = "menus", key = "#p0")
    })
    public Restaurant addMenuItem(String restaurantId, MenuItemRequest request) {
        @SuppressWarnings("null")
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantException("Restaurant not found", HttpStatus.NOT_FOUND));

        MenuItem newItem = new MenuItem(
                UUID.randomUUID().toString(),
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.isAvailable());

        restaurant.getMenuItems().add(newItem);
        return restaurantRepository.save(restaurant);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "restaurants", key = "#p0"),
            @CacheEvict(value = "menus", key = "#p0")
    })
    public Restaurant updateMenuItem(String restaurantId, String itemId, MenuItemRequest request) {
        @SuppressWarnings("null")
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantException("Restaurant not found", HttpStatus.NOT_FOUND));

        MenuItem targetItem = restaurant.getMenuItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RestaurantException("Menu item not found", HttpStatus.NOT_FOUND));

        targetItem.setName(request.getName());
        targetItem.setDescription(request.getDescription());
        targetItem.setPrice(request.getPrice());
        targetItem.setAvailable(request.isAvailable());

        return restaurantRepository.save(restaurant);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "restaurants", key = "#p0"),
            @CacheEvict(value = "menus", key = "#p0")
    })
    public Restaurant deleteMenuItem(String restaurantId, String itemId) {
        @SuppressWarnings("null")
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantException("Restaurant not found", HttpStatus.NOT_FOUND));

        boolean removed = restaurant.getMenuItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new RestaurantException("Menu item not found", HttpStatus.NOT_FOUND);
        }

        return restaurantRepository.save(restaurant);
    }

    @Override
    public List<Restaurant> getRestaurantsByCuisine(String cuisine) {
        return restaurantRepository.findByCuisineTypeIgnoreCase(cuisine);
    }

    @Override
    public List<Restaurant> getAllRestaurants() {
        return restaurantRepository.findAll();
    }
}
