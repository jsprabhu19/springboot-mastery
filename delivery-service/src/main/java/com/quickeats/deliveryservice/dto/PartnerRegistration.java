package com.quickeats.deliveryservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PartnerRegistration {

    @NotBlank(message = "Partner ID is required")
    private String partnerId;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    public PartnerRegistration() {}

    public PartnerRegistration(String partnerId, Double latitude, Double longitude) {
        this.partnerId = partnerId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getPartnerId() { return partnerId; }
    public void setPartnerId(String partnerId) { this.partnerId = partnerId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
