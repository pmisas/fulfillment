package com.fulfillment.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.cdimascio.dotenv.Dotenv;

class OrderFlowE2ETest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Dotenv ENV = Dotenv.configure().ignoreIfMissing().load();
    private static final Dotenv E2E_ENV = Dotenv.configure().directory("e2e-tests").ignoreIfMissing().load();

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void orderMovesFromReceivedToExpectedStatusThroughRealCloudFlow() {
        OrderResponse created = createOrder();

        assertThat(created.orderId()).isNotBlank();
        assertThat(created.status()).isEqualTo("RECEIVED");

        String expectedStatus = env("E2E_EXPECTED_ORDER_STATUS_AFTER_RECEIVED", "VALIDATED");
        OrderResponse finalOrder = waitForOrderStatus(created.orderId(), expectedStatus);

        assertThat(finalOrder.status()).isEqualTo(expectedStatus);
    }

    private OrderResponse createOrder() {
        CreateOrderRequest body = new CreateOrderRequest(
            orderItems(),
            envDouble("E2E_ORDER_LAT", 4.7110),
            envDouble("E2E_ORDER_LNG", -74.0721)
        );

        return post("/api/v1/orders", body, Map.of("Idempotency-Key", "e2e-" + UUID.randomUUID()));
    }

    private OrderResponse waitForOrderStatus(String orderId, String expectedStatus) {
        Instant deadline = Instant.now().plus(envDuration("E2E_POLL_TIMEOUT_MS", 600_000));
        OrderResponse last = null;

        while (!Instant.now().isAfter(deadline)) {
            last = get("/api/v1/orders/" + orderId);
            if (expectedStatus.equals(last.status())) {
                return last;
            }
            sleep(envDuration("E2E_POLL_INTERVAL_MS", 15_000));
        }

        throw new AssertionError("Timed out waiting for order " + orderId
            + " to reach " + expectedStatus + ". Last state: " + last);
    }

    private OrderResponse get(String path) {
        HttpRequest request = baseRequest(path).GET().build();
        return send(request);
    }

    private OrderResponse post(String path, Object body, Map<String, String> headers) {
        try {
            HttpRequest.Builder builder = baseRequest(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
            headers.forEach(builder::header);
            return send(builder.build());
        } catch (IOException e) {
            throw new IllegalStateException("Could not serialize request body", e);
        }
    }

    private HttpRequest.Builder baseRequest(String path) {
        return HttpRequest.newBuilder()
            .uri(URI.create(envRequired("ORDER_API_BASE_URL").replaceAll("/$", "") + path))
            .timeout(Duration.ofSeconds(60))
            .header("Authorization", "Bearer " + envRequired("ORDER_OPERATOR_TOKEN"));
    }

    private OrderResponse send(HttpRequest request) {
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AssertionError("HTTP " + request.method() + " " + request.uri()
                    + " failed with " + response.statusCode() + ": " + response.body());
            }
            return JSON.readValue(response.body(), OrderResponse.class);
        } catch (IOException e) {
            throw new IllegalStateException("HTTP request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP request interrupted", e);
        }
    }

    private static List<OrderItem> orderItems() {
        return Arrays.stream(env("E2E_ORDER_ITEMS", "SKU-1:1").split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(value -> {
                String[] parts = value.split(":");
                return new OrderItem(parts[0], Integer.parseInt(parts[1]));
            })
            .toList();
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Polling interrupted", e);
        }
    }

    private static String envRequired(String name) {
        String value = env(name, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = ENV.get(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = E2E_ENV.get(name);
        return value != null && !value.isBlank() ? value : fallback;
    }

    private static double envDouble(String name, double fallback) {
        String value = env(name, null);
        return value == null ? fallback : Double.parseDouble(value);
    }

    private static Duration envDuration(String name, long fallbackMs) {
        String value = env(name, null);
        return Duration.ofMillis(value == null ? fallbackMs : Long.parseLong(value));
    }

    record CreateOrderRequest(List<OrderItem> items, Double lat, Double lng) {
    }

    record OrderItem(String sku, int quantity) {
    }

    record OrderResponse(String orderId, String status) {
    }
}
