package com.fulfillment.orderstateprocesor.application.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillment.orderstateprocesor.domain.exception.OrderNotFoundException;
import com.fulfillment.orderstateprocesor.domain.model.Order;
import com.fulfillment.orderstateprocesor.domain.model.OrderStateHistory;
import com.fulfillment.orderstateprocesor.domain.model.Status;
import com.fulfillment.orderstateprocesor.domain.ports.OrderRepository;
import com.fulfillment.orderstateprocesor.domain.ports.OrderStateTransitionTransaction;
import com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto.ShipmentDeliveredEvent;

import reactor.core.publisher.Mono;

import static com.fulfillment.orderstateprocesor.domain.shared.DomainValidations.requireNonBlank;

@Component
public class OrderDeliveredHandler implements OrderEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderDeliveredHandler.class);

    private final ObjectMapper mapper;
    private final OrderRepository orderRepo;
    private final OrderStateTransitionTransaction transitionTx;

    public OrderDeliveredHandler(
        ObjectMapper mapper,
        OrderRepository orderRepo,
        OrderStateTransitionTransaction transitionTx
    ) {
        this.mapper = mapper;
        this.orderRepo = orderRepo;
        this.transitionTx = transitionTx;
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
        OrderStateHistory history = OrderStateHistory.transition(order.getOrderId(), Status.SHIPPED, Status.DELIVERED);

        return transitionTx.transitionIfCurrentStatus(next, Status.SHIPPED, history)
            .flatMap(saved -> {
                if (!saved) {
                    log.info("Order {} already advanced past SHIPPED (concurrent message), skipping DELIVERED transition",
                        order.getOrderId());
                    return Mono.empty();
                }
                log.info("Order {} -> DELIVERED (shipmentId={})", order.getOrderId(), shipmentId);
                return Mono.empty();
            });
    }

    private ShipmentDeliveredEvent parse(String json) {
        try {
            return mapper.readValue(json, ShipmentDeliveredEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid ShipmentDelivered payload: " + e.getMessage(), e);
        }
    }
}
