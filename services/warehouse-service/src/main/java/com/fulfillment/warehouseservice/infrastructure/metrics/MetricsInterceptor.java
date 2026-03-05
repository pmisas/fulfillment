package com.fulfillment.warehouseservice.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Interceptor to capture custom metrics for HTTP requests.
 * 
 * Metrics published:
 * - http.requests.duration (Timer): latency with percentiles (p50, p90, p95, p99), count, sum, max
 * - http.requests.errors (Counter): error count for 4xx/5xx responses
 * 
 * Tags kept low-cardinality to avoid CloudWatch cost explosion:
 * - method: GET, POST, PUT, DELETE, etc.
 * - endpoint: normalized URI (UUIDs/numbers replaced with {id})
 * - statusClass: 2xx, 3xx, 4xx, 5xx
 */
@Slf4j
@Component
public class MetricsInterceptor implements HandlerInterceptor {

    private final MeterRegistry meterRegistry;
    private static final String TIMER_START_TIME = "timerStartTime";

    public MetricsInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        request.setAttribute(TIMER_START_TIME, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {

        Long startTime = (Long) request.getAttribute(TIMER_START_TIME);
        if (startTime == null) {
            return;
        }

        long duration = System.nanoTime() - startTime;

        String endpoint = normalizeEndpoint(request.getRequestURI());
        String method = request.getMethod();
        String statusClass = getStatusClass(response.getStatus());

        // Timer already tracks count, sum, max + percentiles
        Timer.builder("http.requests.duration")
                .tag("method", method)
                .tag("endpoint", endpoint)
                .tag("statusClass", statusClass)
                .description("HTTP request duration")
                .publishPercentiles(0.5, 0.90, 0.95, 0.99)
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(30))
                .register(meterRegistry)
                .record(duration, TimeUnit.NANOSECONDS);

        // Count errors separately for easy alarming
        if (response.getStatus() >= 400) {
            Counter.builder("http.requests.errors")
                    .tag("method", method)
                    .tag("endpoint", endpoint)
                    .tag("statusClass", statusClass)
                    .description("HTTP error responses (4xx and 5xx)")
                    .register(meterRegistry)
                    .increment();
        }

        // Log slow requests
        long durationMs = TimeUnit.NANOSECONDS.toMillis(duration);
        if (durationMs > 1000) {
            log.warn("Slow request: {} {} - {}ms - {}", method, endpoint, durationMs, response.getStatus());
        }
    }

    /**
     * Normalize URI by replacing dynamic segments (UUIDs and numbers) with {id}.
     * This keeps the metric cardinality low and groups all requests to the same endpoint.
     */
    static String normalizeEndpoint(String uri) {
        return uri
                .replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "/{id}")
                .replaceAll("/\\d+", "/{id}");
    }

    private String getStatusClass(int status) {
        if (status < 200) return "1xx";
        if (status < 300) return "2xx";
        if (status < 400) return "3xx";
        if (status < 500) return "4xx";
        return "5xx";
    }
}
