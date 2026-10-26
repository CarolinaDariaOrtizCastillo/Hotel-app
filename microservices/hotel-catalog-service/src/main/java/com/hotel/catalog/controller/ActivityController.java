package com.hotel.catalog.controller;

import com.hotel.catalog.model.Activity;
import com.hotel.catalog.service.CatalogDataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final CatalogDataService dataService;

    public ActivityController(CatalogDataService dataService) {
        this.dataService = dataService;
    }

    // Endpoint 3: Listar todas las actividades
    @GetMapping
    public ResponseEntity<List<Activity>> getAllActivities(@RequestParam(required = false) String category) {
        List<Activity> list = dataService.getAllActivities();
        if (category != null && !category.isBlank()) {
            list = list.stream()
                    .filter(a -> a.getCategory().equalsIgnoreCase(category))
                    .toList();
        }
        return ResponseEntity.ok(list);
    }

    // Endpoint 4: Consultar una actividad por ID
    @GetMapping("/{id}")
    public ResponseEntity<Activity> getActivityById(@PathVariable String id) {
        return dataService.getActivityById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Endpoint adicional: Crear una nueva actividad
    @PostMapping
    public ResponseEntity<Activity> createActivity(@RequestBody Activity activity) {
        Activity created = dataService.saveActivity(activity);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Endpoint de control: Modificar delay artificial (para Casuística 2)
    @PostMapping("/admin/delay")
    public ResponseEntity<Map<String, Object>> updateDelay(@RequestParam long delayMs) {
        dataService.updateArtificialDelay(delayMs);
        return ResponseEntity.ok(Map.of(
                "message", "Artificial delay updated successfully",
                "delayMs", delayMs
        ));
    }

    @GetMapping("/admin/delay")
    public ResponseEntity<Map<String, Object>> getDelay() {
        return ResponseEntity.ok(Map.of(
                "currentDelayMs", dataService.getArtificialDelay()
        ));
    }
}
