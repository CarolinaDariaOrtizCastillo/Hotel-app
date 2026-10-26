package com.hotel.booking.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController {

    private final MeterRegistry meterRegistry;

    public MetricsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping(value = "/metrics", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> metrics() {
        if (meterRegistry instanceof PrometheusMeterRegistry prometheusRegistry) {
            return ResponseEntity.ok(prometheusRegistry.scrape());
        }
        return ResponseEntity.ok("# Metrics registry loaded: " + meterRegistry.getClass().getSimpleName());
    }
}
