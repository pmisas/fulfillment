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
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateHistoryRepository;
import com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto.OrderCancelledEvent;

import reactor.core.publisher.Mono;

import static com.fulfillment.orderstateprocesor.domain.shared.DomainValidations.requireNonBlank;

@Component
public class OrderCancelledHandler implements OrderEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderCancelledHandler.class);

    private final ObjectMapper mapper;
    private final OrderRepository orderRepo;
    private final OrderStateHistoryRepository historyRepo;
    private final InventoryClient inventoryClient;

    public OrderCancelledHandler(
        ObjectMapper mapper,
        OrderRepository orderRepo,
        OrderStateHistoryRepository historyRepo,
        InventoryClient inventoryClient
    ) {
        this.mapper = mapper;
        this.orderRepo = orderRepo;
        this.historyRepo = historyRepo;
        this.inventoryClient = inventoryClient;
    }

    @Override
    public String eventType() {
        return "OrderCancelled";
    }

    @Override
    public Mono<Void> handle(String payload) {
        OrderCancelledEvent event = parse(payload);
        String orderId = requireNonBlank(event.orderId(), "orderId");
        String reason = event.reason() == null ? "UNKNOWN" : event.reason();

        String reservationId = "resv:" + orderId;

        log.info("Processing OrderCancelled for order={}, reason={}", orderId, reason);

        return orderRepo.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
            .flatMap(order -> {

                log.info("Order {} current status={}", orderId, order.getStatus());

                if (order.getStatus() == Status.CANCELED) {
                    log.info("Order {} already CANCELED, skipping (idempotency)", orderId);
                    return Mono.just("idempotent").then();
                }

                if (order.getStatus() == Status.SHIPPED) {
                    log.warn("Order {} is SHIPPED, cannot cancel. Skipping.", orderId);
                    return Mono.just("not-cancellable").then();
                }

                if (order.getStatus() == Status.REJECTED) {
                    log.warn("Order {} is REJECTED, no need to cancel. Skipping.", orderId);
                    return Mono.just("already-rejected").then();
                }

                if (!isCancellable(order.getStatus())) {
                    log.warn("Order {} status={} is not cancellable, skipping.", orderId, order.getStatus());
                    return Mono.just("not-cancellable").then();
                }

                return inventoryClient.releaseReservation(reservationId)
                    .doOnSuccess(v -> log.info("Released reservation {} for order={}", reservationId, orderId))
                    .onErrorResume(ex -> {
                        log.error("Failed to release reservation {} for order={}: {}", 
                                  reservationId, orderId, ex.getMessage());
                        return Mono.error(ex);
                    })
                    .then(persistCancelled(order, reason));
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

        log.info("Attempting to cancel order={} from status={}", current.getOrderId(), current.getStatus());

        return orderRepo.saveIfStatusIs(cancelled, current.getStatus())
            .flatMap(saved -> {
                if (!saved) {
                    log.info("Order {} was updated concurrently (status no longer {}), skipping CANCELED transition",
                             current.getOrderId(), current.getStatus());
                    return Mono.just("concurrent-update").then();
                }

                log.info("Order {} successfully marked as CANCELED", current.getOrderId());

                return historyRepo.append(
                    OrderStateHistory.transition(current.getOrderId(), current.getStatus(), Status.CANCELED)
                ).doOnSuccess(v -> log.info("Order {} transitioned {} -> CANCELED, reason={}",
                                            current.getOrderId(), current.getStatus(), reason));
            });
    }

    private OrderCancelledEvent parse(String json) {
        try {
            return mapper.readValue(json, OrderCancelledEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid OrderCancelled payload: " + e.getMessage(), e);
        }
    }
}