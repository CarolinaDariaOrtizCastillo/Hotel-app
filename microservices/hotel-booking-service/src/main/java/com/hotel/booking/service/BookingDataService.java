package com.hotel.booking.service;

import com.hotel.booking.client.CatalogClient;
import com.hotel.booking.metrics.BookingMetrics;
import com.hotel.booking.model.Booking;
import com.hotel.booking.model.BookingRequest;
import com.hotel.booking.model.CatalogResourceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BookingDataService {

    private static final Logger log = LoggerFactory.getLogger(BookingDataService.class);

    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    private final CatalogClient catalogClient;
    private final BookingMetrics metrics;

    public BookingDataService(CatalogClient catalogClient, BookingMetrics metrics) {
        this.catalogClient = catalogClient;
        this.metrics = metrics;
    }

    public Booking createBooking(BookingRequest req) {
        // Validación básica
        if (req.getResourceId() == null || req.getResourceId().isBlank()) {
            throw new IllegalArgumentException("resourceId es obligatorio");
        }
        if (req.getClientEmail() == null || req.getClientEmail().isBlank()) {
            throw new IllegalArgumentException("clientEmail es obligatorio");
        }

        String type = req.getType() != null && req.getType().equalsIgnoreCase("ACTIVITY") ? "ACTIVITY" : "ROOM";

        // Regla del PDF: El Servicio B DEBE consultar al Servicio A para tomar decisiones de negocio
        Optional<CatalogResourceDto> resourceOpt = catalogClient.fetchResource(type, req.getResourceId());
        if (resourceOpt.isEmpty()) {
            throw new NoSuchElementException("El recurso " + req.getResourceId() + " (" + type + ") no existe en el catálogo");
        }

        CatalogResourceDto resource = resourceOpt.get();
        double unitPrice = resource.getEffectivePrice();
        int quantity = req.getQuantity();
        double totalPrice = unitPrice * quantity;

        String id = "bk-" + UUID.randomUUID().toString().substring(0, 8);
        Booking booking = new Booking(
                id,
                req.getClientName() != null ? req.getClientName() : "Huésped Anónimo",
                req.getClientEmail(),
                type,
                req.getResourceId(),
                resource.getName(),
                unitPrice,
                quantity,
                totalPrice,
                req.getDate() != null ? req.getDate() : "2026-11-01",
                "CONFIRMED"
        );

        bookings.put(id, booking);
        metrics.incrementBookingsCreated();
        updateActiveGauge();

        log.info("Reserva exitosa creada con ID: {}, total: ${}", id, totalPrice);
        return booking;
    }

    public List<Booking> getAllBookings() {
        return new ArrayList<>(bookings.values());
    }

    public Optional<Booking> getBookingById(String id) {
        return Optional.ofNullable(bookings.get(id));
    }

    public List<Booking> getBookingsByClient(String email) {
        return bookings.values().stream()
                .filter(b -> b.getClientEmail().equalsIgnoreCase(email))
                .toList();
    }

    public Optional<Booking> cancelBooking(String id) {
        Booking booking = bookings.get(id);
        if (booking != null) {
            booking.setStatus("CANCELLED");
            updateActiveGauge();
            log.info("Reserva {} cancelada", id);
            return Optional.of(booking);
        }
        return Optional.empty();
    }

    private void updateActiveGauge() {
        long activeCount = bookings.values().stream()
                .filter(b -> "CONFIRMED".equalsIgnoreCase(b.getStatus()))
                .count();
        metrics.setActiveBookingsCount((int) activeCount);
    }
}
