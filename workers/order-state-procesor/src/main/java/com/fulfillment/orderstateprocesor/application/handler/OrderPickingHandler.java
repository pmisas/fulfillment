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
import com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto.WarehouseOrderActionEvent;

import reactor.core.publisher.Mono;

import static com.fulfillment.orderstateprocesor.domain.shared.DomainValidations.requireNonBlank;

@Component
public class OrderPickingHandler implements OrderEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderPickingHandler.class);

    private final ObjectMapper mapper;
    private final OrderRepository orderRepo;
    private final OrderStateHistoryRepository historyRepo;

    public OrderPickingHandler(
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
        return "Picked";
    }

    @Override
    public Mono<Void> handle(String payload) {
        WarehouseOrderActionEvent event = parse(payload);
        String orderId = requireNonBlank(event.orderId(), "orderId");
        String warehouseId = requireNonBlank(event.warehouseId(), "warehouseId");

        return orderRepo.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
            .flatMap(order -> applyPickingStarted(order, warehouseId));
    }

    private Mono<Void> applyPickingStarted(Order order, String warehouseId) {
        if (order.getStatus() == Status.PICKED || order.getStatus() == Status.PACKED
                || order.getStatus() == Status.SHIPPED || order.getStatus() == Status.CANCELED) {
            return Mono.empty();
        }

        if (order.getStatus() != Status.VALIDATED) {
            log.warn("Ignoring Picked for order {} because status is {}", order.getOrderId(), order.getStatus());
            return Mono.empty();
        }

        Order withWh = (order.getWarehouseId() == null || order.getWarehouseId().isBlank())
            ? order.withWarehouse(warehouseId)
            : order;

        if (withWh.getWarehouseId() != null && !withWh.getWarehouseId().isBlank()
                && !withWh.getWarehouseId().equals(warehouseId)) {
            log.warn("Ignoring Picked for order {} because warehouse mismatch orderWh={} eventWh={}",
                order.getOrderId(), withWh.getWarehouseId(), warehouseId);
            return Mono.empty();
        }

        Order next = withWh.withStatus(Status.PICKED);

        return orderRepo.save(next)
            .then(historyRepo.append(OrderStateHistory.transition(order.getOrderId(), Status.VALIDATED, Status.PICKED)))
            .doOnSuccess(v -> log.info("Order {} -> PICKED (warehouse={})", order.getOrderId(), warehouseId));
    }

    private WarehouseOrderActionEvent parse(String json) {
        try {
            return mapper.readValue(json, WarehouseOrderActionEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Picked payload: " + e.getMessage(), e);
        }
    }
}