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

import java.util.concurrent.TimeUnit;

/**
 * Interceptor to capture custom metrics for HTTP requests
 * Tracks: response codes, request duration, endpoint traffic
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
        
        // Store start time for latency calculation
        request.setAttribute(TIMER_START_TIME, System.nanoTime());
        
        // Count incoming request
        Counter.builder("http.requests.incoming")
                .tag("method", request.getMethod())
                .tag("endpoint", getEndpoint(request))
                .description("Total incoming HTTP requests")
                .register(meterRegistry)
                .increment();
        
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {
        
        // Calculate request duration
        Long startTime = (Long) request.getAttribute(TIMER_START_TIME);
        if (startTime != null) {
            long duration = System.nanoTime() - startTime;
            
            String endpoint = getEndpoint(request);
            String method = request.getMethod();
            String statusCode = String.valueOf(response.getStatus());
            String statusClass = getStatusClass(response.getStatus());
            
            // Record request duration
            Timer.builder("http.requests.duration")
                    .tag("method", method)
                    .tag("endpoint", endpoint)
                    .tag("status", statusCode)
                    .tag("statusClass", statusClass)
                    .description("HTTP request duration in milliseconds")
                    .register(meterRegistry)
                    .record(duration, TimeUnit.NANOSECONDS);
            
            // Count responses by status code
            Counter.builder("http.responses.status")
                    .tag("method", method)
                    .tag("endpoint", endpoint)
                    .tag("status", statusCode)
                    .tag("statusClass", statusClass)
                    .description("HTTP responses by status code")
                    .register(meterRegistry)
                    .increment();
            
            // Count errors (4xx and 5xx)
            if (response.getStatus() >= 400) {
                Counter.builder("http.requests.errors")
                        .tag("method", method)
                        .tag("endpoint", endpoint)
                        .tag("status", statusCode)
                        .tag("statusClass", statusClass)
                        .description("HTTP error responses")
                        .register(meterRegistry)
                        .increment();
            }
            
            // Log if request took too long
            long durationMs = TimeUnit.NANOSECONDS.toMillis(duration);
            if (durationMs > 1000) {
                log.warn("Slow request detected: {} {} - Duration: {}ms - Status: {}",
                        method, endpoint, durationMs, statusCode);
            }
        }
    }

    private String getEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Remove IDs and dynamic parts for better grouping
        return uri.replaceAll("/\\d+", "/{id}")
                  .replaceAll("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "/{uuid}");
    }

    private String getStatusClass(int status) {
        if (status < 200) return "1xx";
        if (status < 300) return "2xx";
        if (status < 400) return "3xx";
        if (status < 500) return "4xx";
        return "5xx";
    }
}
