package com.quickeats.restaurantservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a request to register or modify a restaurant.
 */
public class RestaurantRequest {

    @NotBlank(message = "Restaurant name is required")
    private String name;

    @NotBlank(message = "Cuisine type is required")
    private String cuisineType;

    @NotBlank(message = "Address is required")
    private String address;

    @NotNull(message = "Longitude coordinate is required")
    private Double longitude;

    @NotNull(message = "Latitude coordinate is required")
    private Double latitude;

    private List<MenuItemRequest> menuItems = new ArrayList<>();

    public RestaurantRequest() {}

    public RestaurantRequest(String name, String cuisineType, String address, Double longitude, Double latitude, List<MenuItemRequest> menuItems) {
        this.name = name;
        this.cuisineType = cuisineType;
        this.address = address;
        this.longitude = longitude;
        this.latitude = latitude;
        if (menuItems != null) {
            this.menuItems = menuItems;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCuisineType() {
        return cuisineType;
    }

    public void setCuisineType(String cuisineType) {
        this.cuisineType = cuisineType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public List<MenuItemRequest> getMenuItems() {
        return menuItems;
    }

    public void setMenuItems(List<MenuItemRequest> menuItems) {
        this.menuItems = menuItems;
    }
}
