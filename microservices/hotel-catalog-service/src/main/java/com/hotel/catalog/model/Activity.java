package com.hotel.catalog.model;

public class Activity {
    private String id;
    private String name;
    private String category; // SPA, TOUR, DINING, ADVENTURE
    private int durationMinutes;
    private int maxCapacity;
    private double price;
    private String schedule;
    private boolean active;

    public Activity() {}

    public Activity(String id, String name, String category, int durationMinutes, int maxCapacity, double price, String schedule, boolean active) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.durationMinutes = durationMinutes;
        this.maxCapacity = maxCapacity;
        this.price = price;
        this.schedule = schedule;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
