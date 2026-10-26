package com.hotel.booking.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
public class BookingMetrics {

    private final MeterRegistry registry;
    private final Counter bookingsCreatedCounter;
    private final Counter bookingsFailedTimeoutCounter;
    private final Counter bookingsFailedGeneralCounter;
    private final Timer catalogCallTimer;
    private final AtomicInteger activeBookingsGaugeValue = new AtomicInteger(0);

    public BookingMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Counter: Total de reservas exitosas creadas
        this.bookingsCreatedCounter = Counter.builder("hotel_booking_created_total")
                .description("Total de reservas y citas creadas con éxito")
                .tag("service", "hotel-booking-service")
                .register(registry);

        // Counter: Fallos de reserva por timeout contra Servicio A (Casuística 2)
        this.bookingsFailedTimeoutCounter = Counter.builder("hotel_booking_failures_total")
                .description("Total de intentos de reserva fallidos")
                .tag("service", "hotel-booking-service")
                .tag("reason", "timeout")
                .register(registry);

        // Counter: Fallos de reserva por otras causas (no encontrado, etc.)
        this.bookingsFailedGeneralCounter = Counter.builder("hotel_booking_failures_total")
                .description("Total de intentos de reserva fallidos")
                .tag("service", "hotel-booking-service")
                .tag("reason", "catalog_error")
                .register(registry);

        // Timer: Tiempo de respuesta en la comunicación HTTP hacia Servicio A
        this.catalogCallTimer = Timer.builder("hotel_booking_catalog_call_duration_seconds")
                .description("Duracion de llamadas WebClient hacia el servicio de catalogo")
                .tag("service", "hotel-booking-service")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // Gauge: Cantidad de reservas activas en memoria
        Gauge.builder("hotel_booking_active_total", activeBookingsGaugeValue, AtomicInteger::get)
                .description("Numero actual de reservas activas (no canceladas) en memoria")
                .tag("service", "hotel-booking-service")
                .register(registry);
    }

    public void incrementBookingsCreated() {
        bookingsCreatedCounter.increment();
    }

    public void incrementFailuresTimeout() {
        bookingsFailedTimeoutCounter.increment();
    }

    public void incrementFailuresGeneral() {
        bookingsFailedGeneralCounter.increment();
    }

    public Timer getCatalogCallTimer() {
        return catalogCallTimer;
    }

    public <T> T recordCatalogCall(Supplier<T> task) {
        return catalogCallTimer.record(task);
    }

    public void setActiveBookingsCount(int count) {
        this.activeBookingsGaugeValue.set(count);
    }
}
