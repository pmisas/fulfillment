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
import com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto.ShipmentDeliveredEvent;

import reactor.core.publisher.Mono;

import static com.fulfillment.orderstateprocesor.domain.shared.DomainValidations.requireNonBlank;

@Component
public class OrderDeliveredHandler implements OrderEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderDeliveredHandler.class);

    private final ObjectMapper mapper;
    private final OrderRepository orderRepo;
    private final OrderStateHistoryRepository historyRepo;

    public OrderDeliveredHandler(
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
        return "ShipmentDelivered";
    }

    @Override
    public Mono<Void> handle(String payload) {
        ShipmentDeliveredEvent event = parse(payload);
        String orderId = requireNonBlank(event.orderId(), "orderId");

        return orderRepo.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
            .flatMap(order -> applyDelivered(order, event.shipmentId()));
    }

    private Mono<Void> applyDelivered(Order order, String shipmentId) {
        if (order.getStatus() == Status.DELIVERED) {
            log.info("Order {} already DELIVERED, skipping (idempotent)", order.getOrderId());
            return Mono.empty();
        }

        if (order.getStatus() != Status.SHIPPED) {
            log.warn("Ignoring ShipmentDelivered for order {} because status is {} (expected SHIPPED)",
                order.getOrderId(), order.getStatus());
            return Mono.empty();
        }

        Order next = order.withStatus(Status.DELIVERED);

        return orderRepo.save(next)
            .then(historyRepo.append(OrderStateHistory.transition(order.getOrderId(), Status.SHIPPED, Status.DELIVERED)))
            .doOnSuccess(v -> log.info("Order {} -> DELIVERED (shipmentId={})", order.getOrderId(), shipmentId));
    }

    private ShipmentDeliveredEvent parse(String json) {
        try {
            return mapper.readValue(json, ShipmentDeliveredEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ShipmentDelivered payload: " + e.getMessage(), e);
        }
    }
}
