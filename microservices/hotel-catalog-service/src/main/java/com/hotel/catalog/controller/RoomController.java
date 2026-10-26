package com.hotel.catalog.controller;

import com.hotel.catalog.model.Room;
import com.hotel.catalog.service.CatalogDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final CatalogDataService dataService;

    public RoomController(CatalogDataService dataService) {
        this.dataService = dataService;
    }

    // Endpoint 1: Listar todas las habitaciones
    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms(@RequestParam(required = false) String type) {
        List<Room> rooms = dataService.getAllRooms();
        if (type != null && !type.isBlank()) {
            rooms = rooms.stream()
                    .filter(r -> r.getType().equalsIgnoreCase(type))
                    .toList();
        }
        return ResponseEntity.ok(rooms);
    }

    // Endpoint 2: Consultar una habitación por ID
    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable String id) {
        return dataService.getRoomById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Endpoint adicional: Crear una nueva habitación en catálogo
    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        Room created = dataService.saveRoom(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
