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
public class OrderPackedHandler implements OrderEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderPackedHandler.class);

    private final ObjectMapper mapper;
    private final OrderRepository orderRepo;
    private final OrderStateHistoryRepository historyRepo;

    public OrderPackedHandler(
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
        return "OrderPacked";
    }

    @Override
    public Mono<Void> handle(String payload) {
        WarehouseOrderActionEvent event = parse(payload);
        String orderId = requireNonBlank(event.orderId(), "orderId");
        String warehouseId = requireNonBlank(event.warehouseId(), "warehouseId");

        return orderRepo.findById(orderId)
            .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
            .flatMap(order -> applyOrderPacked(order, warehouseId));
    }

    private Mono<Void> applyOrderPacked(Order order, String warehouseId) {
        if (order.getStatus() == Status.PACKED
                || order.getStatus() == Status.SHIPPED || order.getStatus() == Status.CANCELED) {
            return Mono.empty();
        }

        if (order.getStatus() != Status.PICKED) {
            log.warn("Ignoring OrderPacked for order {} because status is {}", order.getOrderId(), order.getStatus());
            return Mono.empty();
        }

        if (order.getWarehouseId() == null || order.getWarehouseId().isBlank()
                || !order.getWarehouseId().equals(warehouseId)) {
            log.warn("Ignoring OrderPacked for order {} because warehouse mismatch orderWh={} eventWh={}",
                order.getOrderId(), order.getWarehouseId(), warehouseId);
            return Mono.empty();
        }

        Order next = order.withStatus(Status.PACKED);

        return orderRepo.save(next)
            .then(historyRepo.append(OrderStateHistory.transition(order.getOrderId(), Status.PICKED, Status.PACKED)))
            .doOnSuccess(v -> log.info("Order {} -> PACKED (warehouse={})", order.getOrderId(), warehouseId));
    }

    private WarehouseOrderActionEvent parse(String json) {
        try {
            return mapper.readValue(json, WarehouseOrderActionEvent.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid PackingStarted payload: " + e.getMessage(), e);
        }
    }
}
