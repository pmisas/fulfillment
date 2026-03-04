package com.fulfillment.orderservice.infrastructure.rest.dto;

/**
 * Response for asynchronous operations that are accepted but not yet completed.
 * Used for operations that trigger background processing via events.
 */
public record AsyncOperationResponse(
    String orderId,
    String message,
    String status
) {
    public static AsyncOperationResponse cancellationRequested(String orderId) {
        return new AsyncOperationResponse(
            orderId,
            "Order cancellation has been requested and is being processed. The order will be cancelled shortly and inventory will be released.",
            "PROCESSING"
        );
    }
}
