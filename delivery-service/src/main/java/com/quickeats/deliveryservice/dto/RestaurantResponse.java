package com.quickeats.deliveryservice.dto;

public class RestaurantResponse {
    private String id;
    private String name;
    private double[] location; // [longitude, latitude]

    public RestaurantResponse() {}

    public RestaurantResponse(String id, String name, double[] location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double[] getLocation() { return location; }
    public void setLocation(double[] location) { this.location = location; }
}
