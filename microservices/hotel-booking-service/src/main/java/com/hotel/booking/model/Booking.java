package com.hotel.booking.model;

import java.time.Instant;

public class Booking {
    private String id;
    private String clientName;
    private String clientEmail;
    private String type; // ROOM o ACTIVITY
    private String resourceId;
    private String resourceName;
    private double unitPrice;
    private int quantity;
    private double totalPrice;
    private String date; // ej. 2026-11-01
    private String status; // CONFIRMED, CANCELLED
    private String createdAt;

    public Booking() {}

    public Booking(String id, String clientName, String clientEmail, String type, String resourceId,
                   String resourceName, double unitPrice, int quantity, double totalPrice,
                   String date, String status) {
        this.id = id;
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.type = type;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.date = date;
        this.status = status;
        this.createdAt = Instant.now().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
