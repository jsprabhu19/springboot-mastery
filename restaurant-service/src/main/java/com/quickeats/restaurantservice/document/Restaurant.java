package com.quickeats.restaurantservice.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB Document representing a Restaurant.
 * Menus are embedded directly as nested subdocuments to optimize reads.
 */
@Document(collection = "restaurants")
public class Restaurant {

    @Id
    private String id;
    private String name;
    private String cuisineType;
    private String address;

    @GeoSpatialIndexed
    private double[] location; // [longitude, latitude] for geo-spatial indexing

    private List<MenuItem> menuItems = new ArrayList<>();

    public Restaurant() {}

    public Restaurant(String id, String name, String cuisineType, String address, double[] location, List<MenuItem> menuItems) {
        this.id = id;
        this.name = name;
        this.cuisineType = cuisineType;
        this.address = address;
        this.location = location;
        if (menuItems != null) {
            this.menuItems = menuItems;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public double[] getLocation() {
        return location;
    }

    public void setLocation(double[] location) {
        this.location = location;
    }

    public List<MenuItem> getMenuItems() {
        return menuItems;
    }

    public void setMenuItems(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private String cuisineType;
        private String address;
        private double[] location;
        private List<MenuItem> menuItems = new ArrayList<>();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder cuisineType(String cuisineType) {
            this.cuisineType = cuisineType;
            return this;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder location(double longitude, double latitude) {
            this.location = new double[]{longitude, latitude};
            return this;
        }

        public Builder menuItems(List<MenuItem> menuItems) {
            this.menuItems = menuItems;
            return this;
        }

        public Restaurant build() {
            return new Restaurant(id, name, cuisineType, address, location, menuItems);
        }
    }
}
