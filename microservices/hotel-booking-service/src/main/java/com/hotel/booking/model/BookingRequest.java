package com.hotel.booking.model;

public class BookingRequest {
    private String clientName;
    private String clientEmail;
    private String type; // ROOM o ACTIVITY
    private String resourceId;
    private int quantity;
    private String date; // Fecha de reserva o cita

    public BookingRequest() {}

    public BookingRequest(String clientName, String clientEmail, String type, String resourceId, int quantity, String date) {
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.type = type;
        this.resourceId = resourceId;
        this.quantity = quantity;
        this.date = date;
    }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public int getQuantity() { return quantity <= 0 ? 1 : quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
