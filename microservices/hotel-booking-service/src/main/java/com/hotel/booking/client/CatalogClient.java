package com.hotel.booking.client;

import com.hotel.booking.metrics.BookingMetrics;
import com.hotel.booking.model.CatalogResourceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Component
public class CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);

    private final WebClient webClient;
    private final BookingMetrics metrics;
    private final long timeoutMs;

    public CatalogClient(WebClient.Builder builder,
                         @Value("${hotel.catalog.service-url:http://localhost:8081}") String baseUrl,
                         @Value("${hotel.catalog.timeout-ms:2000}") long timeoutMs,
                         BookingMetrics metrics) {
        this.timeoutMs = timeoutMs;
        this.metrics = metrics;
        this.webClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("CatalogClient configurado con URL: {} y timeout: {} ms", baseUrl, timeoutMs);
    }

    public Optional<CatalogResourceDto> fetchResource(String type, String resourceId) {
        String endpoint = "ROOM".equalsIgnoreCase(type)
                ? "/api/v1/rooms/" + resourceId
                : "/api/v1/activities/" + resourceId;

        return metrics.recordCatalogCall(() -> {
            try {
                log.info("Consultando al Servicio A: {}", endpoint);
                CatalogResourceDto dto = webClient.get()
                        .uri(endpoint)
                        .retrieve()
                        .bodyToMono(CatalogResourceDto.class)
                        .timeout(Duration.ofMillis(timeoutMs))
                        .block();

                return Optional.ofNullable(dto);

            } catch (Exception ex) {
                // Verificamos si la causa fue timeout
                Throwable cause = ex.getCause();
                if (ex instanceof TimeoutException || cause instanceof TimeoutException
                        || ex.getMessage() != null && ex.getMessage().toLowerCase().contains("timeout")) {
                    log.error("TIMEOUT comunicando con Servicio A tras {} ms para recurso {}", timeoutMs, resourceId);
                    metrics.incrementFailuresTimeout();
                    throw new CatalogTimeoutException("El servicio de catalogo no respondio dentro del tiempo limite de " + timeoutMs + " ms");
                }

                if (ex instanceof WebClientResponseException resEx) {
                    if (resEx.getStatusCode().value() == 404) {
                        log.warn("Recurso {} no encontrado en el catalogo", resourceId);
                        metrics.incrementFailuresGeneral();
                        return Optional.empty();
                    }
                }

                log.error("Error general consultando Servicio A: {}", ex.getMessage());
                metrics.incrementFailuresGeneral();
                throw new CatalogCommunicationException("Fallo en la comunicacion con el servicio de catalogo: " + ex.getMessage());
            }
        });
    }

    public static class CatalogTimeoutException extends RuntimeException {
        public CatalogTimeoutException(String message) {
            super(message);
        }
    }

    public static class CatalogCommunicationException extends RuntimeException {
        public CatalogCommunicationException(String message) {
            super(message);
        }
    }
}
