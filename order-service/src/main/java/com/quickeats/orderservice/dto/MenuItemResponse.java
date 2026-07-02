package com.quickeats.orderservice.dto;

import java.math.BigDecimal;

public class MenuItemResponse {
    private String id;
    private String name;
    private BigDecimal price;
    private boolean available;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
