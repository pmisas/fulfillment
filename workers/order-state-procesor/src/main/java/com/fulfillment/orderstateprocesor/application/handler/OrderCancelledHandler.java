package com.fulfillment.orderstateprocesor.application.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.orderstateprocesor.domain.exception.OrderNotFoundException;
import com.fulfillment.orderstateprocesor.domain.model.Order;
import com.fulfillment.orderstateprocesor.domain.model.OrderStateHistory;
import com.fulfillment.orderstateprocesor.domain.model.Status;
import com.fulfillment.orderstateprocesor.domain.ports.InventoryClient;
import com.fulfillment.orderstateprocesor.domain.ports.OrderRepository;
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateTransitionTransaction;
import com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto.OrderCancellationRequestedEvent;

import reactor.core.publisher.Mono;

import static com.fulfillment.orderstateprocesor.domain.shared.DomainValidations.requireNonBlank;

@Component
public class OrderCancelledHandler implements OrderEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderCancelledHandler.class);

    private final ObjectMapper mapper;
    private final OrderRepository orderRepo;
    private final OrderStateTransitionTransaction transitionTx;
    private final InventoryClient inventoryClient;

    public OrderCancelledHandler(
        ObjectMapper mapper,
        OrderRepository orderRepo,
        OrderStateTransitionTransaction transitionTx,
        InventoryClient inventoryClient
    ) {
        this.mapper = mapper;
        this.orderRepo = orderRepo;
        this.transitionTx = transitionTx;
        this.inventoryClient = inventoryClient;
    }

    @Override
    public String eventType() {
        return "OrderCancellationRequested";
    }

    @Override
    public Mono<Void> handle(String payload) {
        OrderCancellationRequestedEvent event = parse(payload);
        String orderId = requireNonBlank(event.orderId(), "orderId");
        String reason = event.reason() == null ? "UNKNOWN" : event.reason();

        String reservationId = "resv:" + orderId;

        log.info("Processing OrderCancelled for order={}, reason={}", orderId, reason);

        return orderRepo.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
            .flatMap(order -> {

                log.info("Order {} current status={}", orderId, order.getStatus());

                if (order.getStatus() == Status.CANCELED) {
                    log.info("Order {} already CANCELED (duplicate message - idempotent)", orderId);
                    return releaseInventoryIfNeeded(reservationId, orderId).then();
                }

                if (order.getStatus() == Status.SHIPPED) {
                    log.warn("Order {} is SHIPPED, cannot cancel. Skipping.", orderId);
                    return Mono.empty();
                }

                if (order.getStatus() == Status.REJECTED) {
                    log.warn("Order {} is REJECTED, no inventory to release. Skipping.", orderId);
                    return Mono.empty();
                }

                if (!isCancellable(order.getStatus())) {
                    log.warn("Order {} status={} is not cancellable, skipping.", orderId, order.getStatus());
                    return Mono.empty();
                }

                return releaseInventoryIfNeeded(reservationId, orderId)
                    .then(persistCancelled(order, reason));
            });
    }

    private Mono<Void> releaseInventoryIfNeeded(String reservationId, String orderId) {
        log.info("Attempting to release inventory: reservationId={}, orderId={}", reservationId, orderId);

        return inventoryClient.releaseReservation(reservationId)
            .doOnSuccess(v -> log.info("Successfully released reservation {} for order={}", reservationId, orderId))
            .onErrorResume(ex -> {
                log.warn("Could not release reservation {} for order={}: {} (may not exist or already released)",
                          reservationId, orderId, ex.getMessage());
                return Mono.empty();
            });
    }

    private boolean isCancellable(Status status) {
        return status == Status.RECEIVED
            || status == Status.VALIDATED
            || status == Status.PICKED
            || status == Status.PACKED;
    }

    private Mono<Void> persistCancelled(Order current, String reason) {
        Order cancelled = current.withStatus(Status.CANCELED);
        OrderStateHistory history = OrderStateHistory.transition(
            current.getOrderId(),
            current.getStatus(),
            Status.CANCELED
        );

        log.info("Attempting to cancel order={} from status={}", current.getOrderId(), current.getStatus());

        return transitionTx.transitionIfCurrentStatus(cancelled, current.getStatus(), history)
            .flatMap(saved -> {
                if (!saved) {
                    log.info("Order {} was updated concurrently (status no longer {}), skipping CANCELED transition",
                             current.getOrderId(), current.getStatus());
                    return Mono.empty();
                }

                log.info("Order {} transitioned {} -> CANCELED, reason={}",
                    current.getOrderId(), current.getStatus(), reason);
                return Mono.empty();
            });
    }

    private OrderCancellationRequestedEvent parse(String json) {
        try {
            return mapper.readValue(json, OrderCancellationRequestedEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid OrderCancellationRequested payload: " + e.getMessage(), e);
        }
    }
}
