package com.fulfillment.orderstateprocesor.application.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.orderstateprocesor.domain.exception.OrderNotFoundException;
import com.fulfillment.orderstateprocesor.domain.model.Order;
import com.fulfillment.orderstateprocesor.domain.model.OrderStateHistory;
import com.fulfillment.orderstateprocesor.domain.model.Status;
import com.fulfillment.orderstateprocesor.domain.ports.OrderRepository;
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateHistoryRepository;
import com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto.ShipmentShippedEvent;

import reactor.core.publisher.Mono;

import static com.fulfillment.orderstateprocesor.domain.shared.DomainValidations.requireNonBlank;

@Component
public class OrderShippedHandler implements OrderEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderShippedHandler.class);

    private final ObjectMapper mapper;
    private final OrderRepository orderRepo;
    private final OrderStateHistoryRepository historyRepo;

    public OrderShippedHandler(
        ObjectMapper mapper,
        OrderRepository orderRepo,
        OrderStateHistoryRepository historyRepo
    ) {
        this.mapper = mapper;
        this.orderRepo = orderRepo;
        this.historyRepo = historyRepo;
    }

    @Override
    public String eventType() {
        return "ShipmentShipped";
    }

    @Override
    public Mono<Void> handle(String payload) {
        ShipmentShippedEvent event = parse(payload);
        String orderId = requireNonBlank(event.orderId(), "orderId");

        return orderRepo.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
            .flatMap(order -> applyShipped(order, event.shipmentId()));
    }

    private Mono<Void> applyShipped(Order order, String shipmentId) {
        if (order.getStatus() == Status.SHIPPED) {
            log.info("Order {} already SHIPPED, skipping (idempotent)", order.getOrderId());
            return Mono.empty();
        }

        if (order.getStatus() != Status.PACKED) {
            log.warn("Ignoring ShipmentShipped for order {} because status is {} (expected PACKED)",
                order.getOrderId(), order.getStatus());
            return Mono.empty();
        }

        Order next = order.withStatus(Status.SHIPPED);

        return orderRepo.saveIfStatusIs(next, Status.PACKED)
            .flatMap(saved -> {
                if (!saved) {
                    log.info("Order {} already advanced past PACKED (concurrent message), skipping SHIPPED transition",
                        order.getOrderId());
                    return Mono.<Void>empty();
                }
                return historyRepo.append(OrderStateHistory.transition(order.getOrderId(), Status.PACKED, Status.SHIPPED))
                    .doOnSuccess(v -> log.info("Order {} -> SHIPPED (shipmentId={})", order.getOrderId(), shipmentId));
            });
    }

    private ShipmentShippedEvent parse(String json) {
        try {
            return mapper.readValue(json, ShipmentShippedEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ShipmentShipped payload: " + e.getMessage(), e);
        }
    }
}
