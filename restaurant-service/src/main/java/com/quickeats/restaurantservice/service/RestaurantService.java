package com.quickeats.restaurantservice.service;

import com.quickeats.restaurantservice.document.MenuItem;
import com.quickeats.restaurantservice.document.Restaurant;
import com.quickeats.restaurantservice.dto.MenuItemRequest;
import com.quickeats.restaurantservice.dto.RestaurantRequest;

import java.util.List;

/**
 * Service interface outlining restaurant and menu management business actions.
 */
public interface RestaurantService {
    Restaurant createRestaurant(RestaurantRequest request);
    Restaurant getRestaurantById(String id);
    List<MenuItem> getRestaurantMenu(String restaurantId);
    Restaurant addMenuItem(String restaurantId, MenuItemRequest request);
    Restaurant updateMenuItem(String restaurantId, String itemId, MenuItemRequest request);
    Restaurant deleteMenuItem(String restaurantId, String itemId);
    List<Restaurant> getRestaurantsByCuisine(String cuisine);
    List<Restaurant> getAllRestaurants();
}
