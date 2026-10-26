package com.hotel.booking.controller;

import com.hotel.booking.client.CatalogClient;
import com.hotel.booking.model.Booking;
import com.hotel.booking.model.BookingRequest;
import com.hotel.booking.service.BookingDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingDataService dataService;

    public BookingController(BookingDataService dataService) {
        this.dataService = dataService;
    }

    // Endpoint 1: Crear una reserva (consulta al Servicio A)
    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody BookingRequest req) {
        Booking created = dataService.createBooking(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Endpoint 2: Listar todas las reservas
    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {
        return ResponseEntity.ok(dataService.getAllBookings());
    }

    // Endpoint 3: Consultar reserva por ID
    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable String id) {
        return dataService.getBookingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Endpoint 4: Cancelar una reserva
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Booking> cancelBooking(@PathVariable String id) {
        return dataService.cancelBooking(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Endpoint adicional: Consultar por email de cliente
    @GetMapping("/client/{email}")
    public ResponseEntity<List<Booking>> getBookingsByClient(@PathVariable String email) {
        return ResponseEntity.ok(dataService.getBookingsByClient(email));
    }

    // Manejo controlado de Timeout hacia Servicio A (Requisito PDF Casuística 2)
    @ExceptionHandler(CatalogClient.CatalogTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleTimeout(CatalogClient.CatalogTimeoutException ex) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(Map.of(
                "error", "Gateway Timeout",
                "message", ex.getMessage(),
                "service", "hotel-booking-service",
                "timestamp", Instant.now().toString()
        ));
    }

    // Manejo de error de comunicación general con Servicio A
    @ExceptionHandler(CatalogClient.CatalogCommunicationException.class)
    public ResponseEntity<Map<String, Object>> handleCatalogDown(CatalogClient.CatalogCommunicationException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "Service Unavailable",
                "message", ex.getMessage(),
                "service", "hotel-booking-service",
                "timestamp", Instant.now().toString()
        ));
    }

    // Manejo de recurso no encontrado
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Resource Not Found",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }
}
