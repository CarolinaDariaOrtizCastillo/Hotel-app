package com.hotel.catalog.model;

public class Room {
    private String id;
    private String code;
    private String name;
    private String type; // SIMPLE, DOUBLE, SUITE, DELUXE
    private int capacity;
    private double pricePerNight;
    private String description;
    private boolean available;

    public Room() {}

    public Room(String id, String code, String name, String type, int capacity, double pricePerNight, String description, boolean available) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.type = type;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
        this.description = description;
        this.available = available;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
