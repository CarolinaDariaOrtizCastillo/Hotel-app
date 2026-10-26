package com.hotel.booking.model;

public class CatalogResourceDto {
    private String id;
    private String name;
    private Double price;
    private Double pricePerNight;
    private boolean available;
    private boolean active;

    public CatalogResourceDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(Double pricePerNight) { this.pricePerNight = pricePerNight; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public double getEffectivePrice() {
        if (pricePerNight != null) return pricePerNight;
        if (price != null) return price;
        return 0.0;
    }
}
