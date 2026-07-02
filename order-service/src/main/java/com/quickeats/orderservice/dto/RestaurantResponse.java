package com.quickeats.orderservice.dto;

import java.util.List;

public class RestaurantResponse {
    private String id;
    private String name;
    private List<MenuItemResponse> menuItems;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<MenuItemResponse> getMenuItems() { return menuItems; }
    public void setMenuItems(List<MenuItemResponse> menuItems) { this.menuItems = menuItems; }
}
