package com.hotel.catalog.service;

import com.hotel.catalog.metrics.CatalogMetrics;
import com.hotel.catalog.model.Activity;
import com.hotel.catalog.model.Room;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CatalogDataService {

    private static final Logger log = LoggerFactory.getLogger(CatalogDataService.class);

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Activity> activities = new ConcurrentHashMap<>();
    private final CatalogMetrics metrics;

    @Value("${hotel.catalog.artificial-delay-ms:0}")
    private long artificialDelayMs;

    public CatalogDataService(CatalogMetrics metrics) {
        this.metrics = metrics;
    }

    @PostConstruct
    public void initData() {
        // Carga inicial en memoria de cuartos
        saveRoom(new Room("room-101", "101", "Habitación Estándar Individual", "SIMPLE", 1, 65.0, "Cama individual, WiFi, TV y baño privado", true));
        saveRoom(new Room("room-102", "102", "Habitación Doble Superior", "DOUBLE", 2, 110.0, "Dos camas Queen, balcón con vista al jardín y minibar", true));
        saveRoom(new Room("room-201", "201", "Suite Familiar Deluxe", "SUITE", 4, 195.0, "Dos dormitorios, sala de estar, jacuzzi y vista al mar", true));
        saveRoom(new Room("room-301", "301", "Suite Presidencial Penthouse", "DELUXE", 6, 380.0, "Terraza privada, piscina climatizada, mayordomo 24/7", true));

        // Carga inicial en memoria de actividades
        saveActivity(new Activity("act-001", "Circuito Spa Termal & Masajes", "SPA", 90, 8, 45.0, "10:00 - 18:00", true));
        saveActivity(new Activity("act-002", "Tour Guiado al Valle y Ruinas", "TOUR", 180, 20, 35.0, "08:30 - 12:00", true));
        saveActivity(new Activity("act-003", "Cena Degustación de 5 Pasos", "DINING", 120, 15, 60.0, "19:30 - 22:30", true));
        saveActivity(new Activity("act-004", "Kayak y Aventura en el Lago", "ADVENTURE", 120, 10, 40.0, "09:00 - 15:00", true));

        metrics.setTotalRoomsCount(rooms.size());
        metrics.setArtificialDelay(this.artificialDelayMs);
        log.info("Catálogo inicializado con {} habitaciones y {} actividades. Delay artificial: {} ms",
                rooms.size(), activities.size(), artificialDelayMs);
    }

    private void applyArtificialDelayIfConfigured() {
        long delay = metrics.getArtificialDelay();
        if (delay > 0) {
            try {
                log.warn("Aplicando delay artificial de {} ms (Casuística 2)", delay);
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Métodos para Habitaciones
    public List<Room> getAllRooms() {
        return metrics.recordLatency(() -> {
            applyArtificialDelayIfConfigured();
            metrics.incrementRoomQueries();
            return new ArrayList<>(rooms.values());
        });
    }

    public Optional<Room> getRoomById(String id) {
        return metrics.recordLatency(() -> {
            applyArtificialDelayIfConfigured();
            metrics.incrementRoomQueries();
            return Optional.ofNullable(rooms.get(id));
        });
    }

    public Room saveRoom(Room room) {
        if (room.getId() == null || room.getId().isBlank()) {
            room.setId("room-" + UUID.randomUUID().toString().substring(0, 8));
        }
        rooms.put(room.getId(), room);
        metrics.setTotalRoomsCount(rooms.size());
        return room;
    }

    // Métodos para Actividades
    public List<Activity> getAllActivities() {
        return metrics.recordLatency(() -> {
            applyArtificialDelayIfConfigured();
            metrics.incrementActivityQueries();
            return new ArrayList<>(activities.values());
        });
    }

    public Optional<Activity> getActivityById(String id) {
        return metrics.recordLatency(() -> {
            applyArtificialDelayIfConfigured();
            metrics.incrementActivityQueries();
            return Optional.ofNullable(activities.get(id));
        });
    }

    public Activity saveActivity(Activity activity) {
        if (activity.getId() == null || activity.getId().isBlank()) {
            activity.setId("act-" + UUID.randomUUID().toString().substring(0, 8));
        }
        activities.put(activity.getId(), activity);
        return activity;
    }

    // Control dinámico de delay artificial (para pruebas / scripts)
    public void updateArtificialDelay(long delayMs) {
        this.artificialDelayMs = delayMs;
        this.metrics.setArtificialDelay(delayMs);
        log.info("Delay artificial actualizado a {} ms", delayMs);
    }

    public long getArtificialDelay() {
        return this.metrics.getArtificialDelay();
    }
}
