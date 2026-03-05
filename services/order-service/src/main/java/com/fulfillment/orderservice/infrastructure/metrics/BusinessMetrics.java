package com.fulfillment.orderservice.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom business metrics for Order Service
 * Use this class to add domain-specific metrics
 */
@Slf4j
@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicInteger> activeOrdersByStatus;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.activeOrdersByStatus = new ConcurrentHashMap<>();
        
        // Register gauges for active orders by status
        Gauge.builder("orders.active.by.status", activeOrdersByStatus, map -> map.values().stream()
                .mapToInt(AtomicInteger::get)
                .sum())
                .description("Total active orders across all statuses")
                .register(meterRegistry);
    }

    /**
     * Record order creation metric
     */
    public void recordOrderCreated(String orderType, double orderValue) {
        Counter.builder("orders.created")
                .tag("type", orderType)
                .description("Total orders created")
                .register(meterRegistry)
                .increment();
        
        // Record order value
        meterRegistry.summary("orders.value")
                .record(orderValue);
        
        log.info("Order created: type={}, value={}", orderType, orderValue);
    }

    /**
     * Record order processing time
     */
    public Timer.Sample startOrderProcessing() {
        return Timer.start(meterRegistry);
    }

    public void stopOrderProcessing(Timer.Sample sample, String orderType, boolean success) {
        sample.stop(Timer.builder("orders.processing.duration")
                .tag("type", orderType)
                .tag("result", success ? "success" : "failure")
                .description("Order processing duration")
                .register(meterRegistry));
    }

    /**
     * Record order status change
     */
    public void recordOrderStatusChange(String fromStatus, String toStatus) {
        // Decrement old status count
        activeOrdersByStatus.computeIfAbsent(fromStatus, k -> new AtomicInteger(0))
                .decrementAndGet();
        
        // Increment new status count
        activeOrdersByStatus.computeIfAbsent(toStatus, k -> new AtomicInteger(0))
                .incrementAndGet();
        
        Counter.builder("orders.status.transitions")
                .tag("from", fromStatus)
                .tag("to", toStatus)
                .description("Order status transitions")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record order cancellation
     */
    public void recordOrderCancelled(String reason) {
        Counter.builder("orders.cancelled")
                .tag("reason", reason)
                .description("Orders cancelled by reason")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record payment processing
     */
    public void recordPaymentProcessed(String paymentMethod, boolean success) {
        Counter.builder("payments.processed")
                .tag("method", paymentMethod)
                .tag("result", success ? "success" : "failure")
                .description("Payment processing attempts")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record external service calls
     */
    public void recordExternalServiceCall(String serviceName, String operation, long durationMs, boolean success) {
        Timer.builder("external.service.calls")
                .tag("service", serviceName)
                .tag("operation", operation)
                .tag("result", success ? "success" : "failure")
                .description("External service call duration")
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Record cache hit/miss
     */
    public void recordCacheAccess(String cacheName, boolean hit) {
        Counter.builder("cache.access")
                .tag("cache", cacheName)
                .tag("result", hit ? "hit" : "miss")
                .description("Cache access patterns")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record database operation
     */
    public void recordDatabaseOperation(String table, String operation, long durationMs) {
        Timer.builder("database.operations")
                .tag("table", table)
                .tag("operation", operation)
                .description("Database operation duration")
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Record validation errors
     */
    public void recordValidationError(String field, String errorType) {
        Counter.builder("validation.errors")
                .tag("field", field)
                .tag("errorType", errorType)
                .description("Validation errors by field and type")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record retry attempts
     */
    public void recordRetryAttempt(String operation, int attemptNumber, boolean success) {
        Counter.builder("operation.retries")
                .tag("operation", operation)
                .tag("attempt", String.valueOf(attemptNumber))
                .tag("result", success ? "success" : "failure")
                .description("Operation retry attempts")
                .register(meterRegistry)
                .increment();
    }
}
