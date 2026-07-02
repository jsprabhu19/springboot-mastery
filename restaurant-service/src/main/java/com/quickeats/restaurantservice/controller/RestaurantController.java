package com.quickeats.restaurantservice.controller;

import com.quickeats.restaurantservice.document.MenuItem;
import com.quickeats.restaurantservice.document.Restaurant;
import com.quickeats.restaurantservice.dto.MenuItemRequest;
import com.quickeats.restaurantservice.dto.RestaurantRequest;
import com.quickeats.restaurantservice.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing restaurant management and menu CRUD endpoints.
 * Controls access via standard PreAuthorize Spring Security role checks.
 */
@RestController
@RequestMapping("/api/v1/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public Restaurant createRestaurant(@Valid @RequestBody RestaurantRequest request) {
        return restaurantService.createRestaurant(request);
    }

    @GetMapping
    public List<Restaurant> getAllRestaurants() {
        return restaurantService.getAllRestaurants();
    }

    @GetMapping("/search")
    public List<Restaurant> searchByCuisine(@RequestParam("cuisine") String cuisine) {
        return restaurantService.getRestaurantsByCuisine(cuisine);
    }

    @GetMapping("/{id}")
    public Restaurant getRestaurantById(@PathVariable("id") String id) {
        return restaurantService.getRestaurantById(id);
    }

    @GetMapping("/{id}/menu")
    public List<MenuItem> getRestaurantMenu(@PathVariable("id") String id) {
        return restaurantService.getRestaurantMenu(id);
    }

    @PostMapping("/{id}/menu")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public Restaurant addMenuItem(@PathVariable("id") String id, @Valid @RequestBody MenuItemRequest request) {
        return restaurantService.addMenuItem(id, request);
    }

    @PutMapping("/{id}/menu/{itemId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public Restaurant updateMenuItem(
            @PathVariable("id") String id,
            @PathVariable("itemId") String itemId,
            @Valid @RequestBody MenuItemRequest request) {
        return restaurantService.updateMenuItem(id, itemId, request);
    }

    @DeleteMapping("/{id}/menu/{itemId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public Restaurant deleteMenuItem(@PathVariable("id") String id, @PathVariable("itemId") String itemId) {
        return restaurantService.deleteMenuItem(id, itemId);
    }
}
