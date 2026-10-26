package com.hotel.catalog.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Component
public class CatalogMetrics {

    private final Counter roomQueriesCounter;
    private final Counter activityQueriesCounter;
    private final Timer requestLatencyTimer;
    private final AtomicLong artificialDelayGaugeValue = new AtomicLong(0);
    private final AtomicInteger totalRoomsGaugeValue = new AtomicInteger(0);

    public CatalogMetrics(MeterRegistry registry,
                          @Value("${hotel.catalog.artificial-delay-ms:0}") long initialDelay) {

        // Counter: Conteo de consultas a habitaciones
        this.roomQueriesCounter = Counter.builder("hotel_catalog_room_queries_total")
                .description("Total de consultas realizadas sobre el catalogo de cuartos")
                .tag("service", "hotel-catalog-service")
                .register(registry);

        // Counter: Conteo de consultas a actividades
        this.activityQueriesCounter = Counter.builder("hotel_catalog_activity_queries_total")
                .description("Total de consultas realizadas sobre el catalogo de actividades")
                .tag("service", "hotel-catalog-service")
                .register(registry);

        // Timer: Medición de latencia de endpoints del catálogo
        this.requestLatencyTimer = Timer.builder("hotel_catalog_request_duration_seconds")
                .description("Tiempo de procesamiento de peticiones en catalogo")
                .tag("service", "hotel-catalog-service")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // Gauge: Delay artificial actual (Casuística 2)
        this.artificialDelayGaugeValue.set(initialDelay);
        Gauge.builder("hotel_catalog_artificial_delay_ms", artificialDelayGaugeValue, AtomicLong::get)
                .description("Delay artificial configurado en el servicio de catalogo para simular degradacion")
                .tag("service", "hotel-catalog-service")
                .register(registry);

        // Gauge: Cantidad de habitaciones registradas en memoria
        Gauge.builder("hotel_catalog_total_rooms_count", totalRoomsGaugeValue, AtomicInteger::get)
                .description("Numero total de habitaciones disponibles en memoria")
                .tag("service", "hotel-catalog-service")
                .register(registry);
    }

    public void incrementRoomQueries() {
        roomQueriesCounter.increment();
    }

    public void incrementActivityQueries() {
        activityQueriesCounter.increment();
    }

    public Timer getRequestLatencyTimer() {
        return requestLatencyTimer;
    }

    public void recordLatency(Runnable task) {
        requestLatencyTimer.record(task);
    }

    public <T> T recordLatency(Supplier<T> task) {
        return requestLatencyTimer.record(task);
    }

    public void setArtificialDelay(long delayMs) {
        this.artificialDelayGaugeValue.set(delayMs);
    }

    public long getArtificialDelay() {
        return this.artificialDelayGaugeValue.get();
    }

    public void setTotalRoomsCount(int count) {
        this.totalRoomsGaugeValue.set(count);
    }
}
